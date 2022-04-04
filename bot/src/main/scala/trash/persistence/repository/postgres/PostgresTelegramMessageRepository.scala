package trash.persistence.repository.postgres

import com.bot4s.telegram.models.Message
import cats.effect.kernel.MonadCancelThrow
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import PostgresTelegramMessageRepository._
import cats.syntax.all._
import cats._
import trash.persistence.model.{DBMessage, MsgType}
import trash.persistence.repository.TelegramMessageRepository

class PostgresTelegramMessageRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends TelegramMessageRepository[F] {

  def getChatMessages(chatId: Long): F[List[DBMessage]] =
    sql"""
      SELECT chat_id, message_id, type, content FROM message
      WHERE chat_id = $chatId
       """.query[DBMessage].to[List].transact(xa)

  def getRandomMessage(chatId: Long): F[List[DBMessage]] =
    sql"""
      SELECT chat_id, message_id, type, content FROM message
      WHERE chat_id = $chatId
      ORDER BY RANDOM()
      LIMIT 1
       """.query[DBMessage].to[List].transact(xa)

  def insertMessage(msg: Message): F[Unit] =
    DBMessage
      .from(msg)
      .map { m =>
        val updateChat = sql"""
          INSERT INTO chat (chat_id) values (${m.chatId}) ON CONFLICT DO NOTHING;
        """.update.run

        val updateMessages = sql"""
          INSERT INTO message (message_id, chat_id, content, type) 
          VALUES (${m.messageId}, ${m.chatId}, ${m.content}, ${m.msgType.toString}::msg_type);
        """.update.run.void

        (updateChat *> updateMessages)
          .transact(xa)
          .void
      }
      .getOrElse(
        Applicative[F].unit
      )

}

object PostgresTelegramMessageRepository {

  def toEnum(t: MsgType): String           = t.toString
  def fromEnum(s: String): Option[MsgType] = MsgType.mapping.get(s)

  implicit val msgTypeMeta: Meta[MsgType] =
    pgEnumStringOpt("msg_type", fromEnum, toEnum)

  def apply[F[_]: MonadCancelThrow](
    xa: Transactor[F]
  ): PostgresTelegramMessageRepository[F] =
    new PostgresTelegramMessageRepository(xa)
}
