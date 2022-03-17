package org.trashtalk.persistence.repository

import org.trashtalk.persistence.model.DBMessage
import com.bot4s.telegram.models.Message

trait TelegramMessageRepository[F[_]] {
  def getChatMessages(chatId: Long): F[List[DBMessage]]
  def getRandomMessage(chatId: Long): F[List[DBMessage]]
  def insertMessage(msg: Message): F[Unit]
}
