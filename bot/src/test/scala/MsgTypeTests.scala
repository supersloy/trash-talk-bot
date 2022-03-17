import org.trashtalk.persistence.model.MsgType
import org.trashtalk.persistence.repository.PostgresTelegramMessageRepository.{
  fromEnum,
  toEnum,
}

class MsgTypeTests extends munit.FunSuite {
  val msgTypeTestCases: Map[MsgType, String] = Map(
    MsgType.TEXT    -> "TEXT",
    MsgType.VIDEO   -> "VIDEO",
    MsgType.DOC     -> "DOC",
    MsgType.STICKER -> "STICKER",
    MsgType.IMAGE   -> "IMAGE",
  )

  msgTypeTestCases.foreach { case (msgType, str) =>
    test(s"converts $msgType to string") {
      assertEquals(toEnum(msgType), str)
    }

    test(s"converts string to $msgType") {
      assertEquals(fromEnum(str), Some(msgType))
    }
  }

  test("MsgType.toEnum returns None when given an invalid string") {
    assertEquals(fromEnum("ABOBA"), None)
  }

}
