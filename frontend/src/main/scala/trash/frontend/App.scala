package trash.frontend

import slinky.web.html._
import slinky.core.facade.Hooks._
import slinky.core.{FunctionalComponent, SyntheticEvent}
import trash.frontend.ChatFind.ChatFindProps
import trash.frontend.MessageList.MessageListProps

object App {
  private val css = Css.App

  val App: FunctionalComponent[Unit] = FunctionalComponent[Unit] { props =>
    val (chatID, setChatID) = useState(0)
    div(
      className := "App",
      div(
        className := "center",
        ChatFind.ChatFind(ChatFindProps(setChatID)),
        MessageList.MessageList(MessageListProps(chatID)),
      ),
    )
  }
}
