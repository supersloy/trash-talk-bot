package org.trashtalk.bot

import cats.effect._
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.trashtalk.bot.Schemas.MsgType
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

class Bot[F[_]: Async](
  token: String,
  backend: SttpBackend[F, Any],
  xa: Transactor[F],
  telegramApi: String = "api.telegram.org",
) extends TelegramBot[F](token, backend, telegramApi)
  with Polling[F]
  with Commands[F] {

  def generateMessage(chatId: Long): F[Unit] =
    SQLCommands
      .getRandomMessage(chatId)
      .transact(xa)
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
      _ <- SQLCommands.insertMessage(msg).transact(xa)
      _ <- generateMessage(msg.chat.id)
    } yield ()

}

object Bot extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      token <- IO.fromOption(sys.env.get("TRASHTALK_TOKEN"))(
        new Exception(
          "Telegram Bot API token is not set. " +
            "Please set the \"TRASHTALK_TOKEN\" environment variable" +
            " with your token value."
        )
      )
      backend <- AsyncHttpClientCatsBackend[IO]()
      xa <- IO.delay(
        Transactor.fromDriverManager[IO](
          driver = "org.postgresql.Driver",
          url = "jdbc:postgresql://localhost:5432/postgres",
          user = "postgres",
          pass = "changeme",
        )
      )
      bot <- IO.pure(new Bot(token, backend, xa))
      botResource = Resource.make(bot.run().start)(_ =>
        IO.blocking(bot.shutdown())
      )
      _ <- botResource.use(_ => IO.never)
    } yield ExitCode.Success

}
