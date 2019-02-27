import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.enum.XdEnumEntityType
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File

class XdAuthor(entity: Entity) : XdEntity(entity) {
  companion object : XdNaturalEntityType<XdAuthor>()

  var name by xdRequiredStringProp()
  var country by xdStringProp()
  var yearOfBirth by xdRequiredIntProp()
  var yearofDeath by xdIntProp()

  val books by xdLink0_N(XdBook, onTargetDelete = OnDeletePolicy.CLEAR)
}

class XdBook(entity: Entity) : XdEntity(entity) {
  companion object : XdNaturalEntityType<XdBook>()

  var title by xdRequiredStringProp()
  var year by xdIntProp()
  val genre by xdLink1_N(XdGenre)

  val authors by xdLink0_N(XdAuthor, onTargetDelete = OnDeletePolicy.CASCADE)
}

class XdGenre(entity: Entity) : XdEnumEntity(entity) {
  companion object : XdEnumEntityType<XdGenre>() {
    val FANTASY by enumField { presentation = "Fantasy" }
    val ROMANCE by enumField { presentation = "Romance" }
    val ADVENTURES by enumField { presentation = "A" }
    val OTHER by enumField { presentation = "-" }
  }

  var presentation by xdRequiredStringProp()
    private set
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

  val author = store.transactional {
    XdAuthor.new {
      name = "Charlotte Brontë"
      country = "England"
      yearOfBirth = 1816
      yearofDeath = 1855
    }
  }

  val book = store.transactional {
    XdBook.new {
      title = "Jane Eyre"
      year = 1847
      genre.add(XdGenre.ROMANCE)
      authors.add(author)
    }
  }

  store.transactional(readonly = true) {
    println(author)
    println(book)
  }
}
