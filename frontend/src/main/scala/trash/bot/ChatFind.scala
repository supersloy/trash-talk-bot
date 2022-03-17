package trash.bot

import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import slinky.core.{FunctionalComponent, SyntheticEvent}
import slinky.core.facade.Hooks.useState
import slinky.core.facade.SetStateHookCallback
import slinky.web.html._

object ChatFind {
  case class ChatFindProps(setChatId: SetStateHookCallback[Int])

  val ChatFind: FunctionalComponent[ChatFindProps] = FunctionalComponent[ChatFindProps] { props =>
    val (inputVal, setInputVal) = useState(0)
    val (errorState, setErrorState) = useState(false)

    def handleChange(e: SyntheticEvent[Input, Event]): Unit = {
      try {
        setInputVal(e.target.value.toInt)
        setErrorState(false)
      } catch {
        case e: Exception => setErrorState(true)
      }
    }

    def handleClick(): Unit = {
      props.setChatId(inputVal)
    }

    div()(
      div(className:="form__group field",
        input(
          className := (if (errorState) "form__field error_input" else "form__field"),
          placeholder := "ChatID",
          onChange := (handleChange(_)),
          name:="chatID",
          id:="chatID",
          required
        ),
        label(
          htmlFor:="chatID",
          className:="form__label",
          "ChatID"
        )
      ),
      button(
        className:="button-6",
        onClick := (_ => handleClick())
      )("Перейти к сообщениям")
    )
  }

}
//<div class="form__group field">
//<input type="input" class="form__field" placeholder="Name" name="name" id='name' required />
//<label for="name" class="form__label">Name</label>
//</div>