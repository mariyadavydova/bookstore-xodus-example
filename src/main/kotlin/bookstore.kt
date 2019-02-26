import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.enum.XdEnumEntityType
import kotlinx.dnq.link.OnDeletePolicy

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
    val FANTASY by enumField { presentation = "F" }
    val ROMANCE by enumField { presentation = "R" }
    val ADVENTURES by enumField { presentation = "A" }
    val OTHER by enumField { presentation = "-" }
  }

  var presentation by xdRequiredStringProp()
    private set
}

fun main() {
  println("Hello World")
}
