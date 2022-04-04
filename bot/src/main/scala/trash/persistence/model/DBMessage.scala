package trash.persistence.model

import com.bot4s.telegram.models.Message

final case class DBMessage(
  chatId: Long,
  messageId: Long,
  msgType: MsgType,
  content: String,
)

object DBMessage {
  def from(msg: Message): Option[DBMessage] =
    ((msg.text, msg.sticker, msg.video, msg.photo, msg.document) match {
      case (Some(text), None, None, None, None) => Some((MsgType.TEXT, text))
      case (None, Some(sticker), None, None, None) =>
        Some((MsgType.STICKER, sticker.fileId))
      case (None, None, Some(video), None, None) =>
        Some((MsgType.VIDEO, video.fileId))
      case (None, None, None, Some(photo +: _), None) =>
        Some((MsgType.IMAGE, photo.fileId))
      case (None, None, None, None, Some(document)) =>
        Some((MsgType.DOC, document.fileId))
      case _ => None
    }).map { case (msgType, content) =>
      DBMessage(msg.chat.id, msg.messageId, msgType, content)
    }
}
