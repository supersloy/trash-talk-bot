import org.trashtalk.bot.Schemas.MsgType

class MsgTypeTests extends munit.FunSuite {
  val msgTypeTestCases: Map[MsgType, String] = Map(
    MsgType.TEXT -> "TEXT",
    MsgType.VIDEO -> "VIDEO",
    MsgType.DOC -> "DOC",
    MsgType.STICKER -> "STICKER",
    MsgType.IMAGE -> "IMAGE",
  )

  msgTypeTestCases.foreach {
    case (msgType, str) => {
      test(s"converts $msgType to string") {
        assertEquals(MsgType.toEnum(msgType), str)
      }

      test(s"converts string to $msgType") {
        assertEquals(MsgType.fromEnum(str), Some(msgType))
      }
    }
  }

  test("MsgType.toEnum returns None when given an invalid string") {
    assertEquals(MsgType.fromEnum("ABOBA"), None)
  }


}
