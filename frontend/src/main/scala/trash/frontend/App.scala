package trash.frontend

import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks._
import slinky.web.html._

object App {
  private val css = Css.App

  val App: FunctionalComponent[Unit] = FunctionalComponent[Unit] { props =>
    val (chatID, setChatID) = useState(0)
    val telegramLoginProps = TelegramLoginButton.Props("","")

    div(
      TelegramLoginButton(p=telegramLoginProps)
    )
  }
}
