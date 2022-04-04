package trash.persistence.repository

import com.bot4s.telegram.models.Message
import trash.persistence.model.DBMessage

trait TelegramMessageRepository[F[_]] {
  def getChatMessages(chatId: Long): F[List[DBMessage]]
  def getRandomMessage(chatId: Long): F[List[DBMessage]]
  def insertMessage(msg: Message): F[Unit]
}
