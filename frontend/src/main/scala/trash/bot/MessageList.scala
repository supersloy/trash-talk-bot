package trash.bot

import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.useEffect
import slinky.web.html._

object MessageList {
  case class MessageListProps(chatID: Int)

  val MessageList = FunctionalComponent[MessageListProps] { props =>
    val a = if (props.chatID != 0) Seq("message 1", "message 2", "message 3") else Seq()
    useEffect(() => {
      //Load data from backend into "a"
    },
      Seq(props.chatID))
    div(
      ul(className := "gradient-list",
        a.map { item =>
          li(key := item)(item)
        }
      )
    )
  }
}
