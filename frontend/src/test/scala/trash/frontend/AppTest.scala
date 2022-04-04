package trash.frontend

import slinky.web.ReactDOM
import org.scalajs.dom.document
import trash.frontend.App.App
import scalajs.js

class AppTest extends munit.FunSuite {
  test("Renders without crashing") {
    val div = document.createElement("div")
    ReactDOM.render(App(), div)
    ReactDOM.unmountComponentAtNode(div)
  }

  test("Button has text") {
    val root = document.createElement("div")
    ReactDOM.render(App(), root)
    val buttonText = root.getElementsByClassName("button-6").lift(0)

    buttonText match {
      case None => fail("There is no button!")
      case Some(value) =>
        assertNoDiff(value.textContent, "Перейти к сообщениям")
    }
    ReactDOM.unmountComponentAtNode(root)
  }
}
