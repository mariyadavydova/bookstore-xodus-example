import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.constraints.length
import kotlinx.dnq.*
import kotlinx.dnq.enum.XdEnumEntityType
import kotlinx.dnq.query.*
import kotlinx.dnq.simple.containsNone
import kotlinx.dnq.simple.max
import kotlinx.dnq.simple.regex
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File

class XdAuthor(entity: Entity) : XdEntity(entity) {
  companion object : XdNaturalEntityType<XdAuthor>()

  var name by xdRequiredStringProp { containsNone("?!") }
  var countryOfBirth by xdStringProp {
    length(min = 3, max = 56)
    regex(Regex("[A-Za-z.,]+"))
  }
  var yearOfBirth by xdRequiredIntProp { max(2019) }
  var yearOfDeath by xdNullableIntProp { max(2019) }
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
      countryOfBirth = "England"
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
      countryOfBirth = "England"
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
      countryOfBirth = "USA"
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

  store.transactional(readonly = true) {
    val fantasyBooks = XdBook.filter { it.genres contains XdGenre.FANTASY }
    val booksOf20thCentury = XdBook.filter { (it.year ge 1900) and (it.year lt 1999) }
    val authorWithMoreThanOneBook = XdAuthor.filter { it.books.size() > 1 }

    val booksSortedByYear = XdBook.all().sortedBy(XdBook::year)

    val allGenres = XdBook.all().flatMapDistinct(XdBook::genres)
  }
}
