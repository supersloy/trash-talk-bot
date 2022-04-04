package trash.frontend
import scalajs.js
import org.scalajs.dom

@js.native
@js.annotation.JSImport("console.amogus", js.annotation.JSImport.Namespace)
private object AmogusImport extends js.Object

@js.native
trait ConsoleAmogus extends js.Object {
  def amogus(
    imposter: String,
    numOfImposters: Int,
    isImposter: js.UndefOr[Boolean],
  ): Unit = js.native
}

object ConsoleAmogus extends js.Object {
  implicit def console2amogus(console: dom.Console): ConsoleAmogus =
    console.asInstanceOf[ConsoleAmogus]
}
