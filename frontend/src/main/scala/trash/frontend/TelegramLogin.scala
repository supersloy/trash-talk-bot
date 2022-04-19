package trash.frontend

import scalajs.js
import org.scalajs.dom
import slinky.core.ExternalComponent
import slinky.core.annotations.react

import scala.scalajs.js.annotation.JSImport

@JSImport("telegram-login-button", JSImport.Default)
@js.native
object TelegramLoginButtonImport extends js.Object {
  val TelegramLoginButton: js.Object = js.native
}

object TelegramLoginButton extends ExternalComponent {
  case class Props(botName: String, dataOnauth: String)
  override val component = TelegramLoginButtonImport.TelegramLoginButton
}
