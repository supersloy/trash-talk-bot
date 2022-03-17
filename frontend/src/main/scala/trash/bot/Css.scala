package trash.bot
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Css {

    @js.native
    @JSImport("/css/index.module.css", JSImport.Default)
    object Index extends js.Object

    @js.native
    @JSImport("/css/App.module.css", JSImport.Default)
    object App extends js.Object
    
}