package trash.bot

import cats.effect._
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._
import doobie.implicits._
import sttp.client3.SttpBackend
import trash.persistence.model.MsgType
import trash.persistence.repository.TelegramMessageRepository

class Bot[F[_]: Async](
  token: String,
  backend: SttpBackend[F, Any],
  repo: TelegramMessageRepository[F],
  telegramApi: String = "api.telegram.org",
) extends TelegramBot[F](token, backend, telegramApi)
  with Polling[F]
  with Commands[F] {

  def generateMessage(chatId: Long): F[Unit] =
    repo
      .getRandomMessage(chatId)
      .flatMap { list =>
        list.headOption match {
          case Some(msg) =>
            val content = msg.msgType match {
              case MsgType.TEXT  => SendMessage(chatId, msg.content)
              case MsgType.IMAGE => SendPhoto(chatId, InputFile(msg.content))
              case MsgType.STICKER =>
                SendSticker(chatId, InputFile(msg.content))
              case MsgType.VIDEO => SendVideo(chatId, InputFile(msg.content))
              case MsgType.DOC   => SendDocument(chatId, InputFile(msg.content))
            }
            request(content).void
          case None =>
            ().pure[F]
        }
      }

  override def receiveMessage(msg: Message): F[Unit] =
    for {
      _ <- Async[F].delay(logger.info(msg.toString))
      _ <- repo.insertMessage(msg)
      _ <- generateMessage(msg.chat.id)
    } yield ()
}
