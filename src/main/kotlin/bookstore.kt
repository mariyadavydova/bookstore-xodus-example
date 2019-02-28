import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.enum.XdEnumEntityType
import kotlinx.dnq.query.XdMutableQuery
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File

class XdAuthor(entity: Entity) : XdEntity(entity) {
  companion object : XdNaturalEntityType<XdAuthor>()

  var name by xdRequiredStringProp()
  var country by xdStringProp()
  var yearOfBirth by xdRequiredIntProp()
  var yearOfDeath by xdNullableIntProp()
  val books by xdLink0_N(XdBook::authors)

  override fun toString(): String {
    val bibliography = books.asSequence().joinToString("\n")
    return "$name ($yearOfBirth-${yearOfDeath ?: "???"}):\n$bibliography"
  }
}

class XdBook(entity: Entity) : XdEntity(entity) {
  companion object : XdNaturalEntityType<XdBook>()

  var title by xdRequiredStringProp()
  var year by xdNullableIntProp()
  val genres by xdLink1_N(XdGenre)
  val authors : XdMutableQuery<XdAuthor> by xdLink1_N(XdAuthor::books)

  override fun toString(): String {
    val genres = genres.asSequence().joinToString(", ")
    return "$title (${year ?: "Unknown"}) - $genres"
  }
}

class XdGenre(entity: Entity) : XdEnumEntity(entity) {
  companion object : XdEnumEntityType<XdGenre>() {
    val FANTASY by enumField {}
    val ROMANCE by enumField {}
  }

  override fun toString(): String {
    return this.name.toLowerCase().capitalize()
  }
}

fun initXodus(): TransientEntityStore {
  XdModel.registerNodes(
      XdAuthor,
      XdBook,
      XdGenre
  )
  val databaseHome = File(System.getProperty("user.home"), "bookstore")
  val store = StaticStoreContainer.init(
      dbFolder = databaseHome,
      environmentName = "db"
  )
  initMetaData(XdModel.hierarchy, store)
  return store
}

fun main() {
  val store = initXodus()

  val bronte = store.transactional {
    XdAuthor.new {
      name = "Charlotte Brontë"
      country = "England"
      yearOfBirth = 1816
      yearOfDeath = 1855
    }
  }

  store.transactional {
    XdBook.new {
      title = "Jane Eyre"
      year = 1847
      genres.add(XdGenre.ROMANCE)
      authors.add(bronte)
    }
  }

  val tolkien = store.transactional {
    XdAuthor.new {
      name = "J. R. R. Tolkien"
      country = "England"
      yearOfBirth = 1892
      yearOfDeath = 1973
    }
  }

  store.transactional {
    tolkien.books.add(XdBook.new {
      title = "The Hobbit"
      year = 1937
      genres.add(XdGenre.FANTASY)
    })
    tolkien.books.add(XdBook.new {
      title = "The Lord of the Rings"
      year = 1955
      genres.add(XdGenre.FANTASY)
    })
  }

  store.transactional {
    XdAuthor.new {
      name = "George R. R. Martin"
      country = "USA"
      yearOfBirth = 1948
      books.add(XdBook.new {
        title = "A Game of Thrones"
        year = 1996
        genres.add(XdGenre.FANTASY)
      })
    }
  }

  store.transactional(readonly = true) {
    println(XdAuthor.all().asSequence().joinToString("\n***\n"))
  }
}
