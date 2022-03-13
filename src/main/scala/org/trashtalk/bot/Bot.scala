package org.trashtalk.bot

import cats.effect._
import cats.syntax.all._
import com.bot4s.telegram.api.declarative.{
  Action,
  CommandFilterMagnet,
  Commands,
}
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

  onCommand("start") { implicit msg =>
    request(SendMessage(msg.chat.chatId, "go fuck yourself")).void
  }

  def generateMessage(chat_id: Long): F[Unit] =
    SQLCommands
      .getRandomMessage(chat_id)
      .transact(xa)
      .flatMap { list =>
        list.headOption match {
          case Some(msg) =>
            val content = msg.msgType match {
              case MsgType.TEXT  => SendMessage(chat_id, msg.content)
              case MsgType.IMAGE => SendPhoto(chat_id, InputFile(msg.content))
              case MsgType.STICKER =>
                SendSticker(chat_id, InputFile(msg.content))
              case MsgType.VIDEO => SendVideo(chat_id, InputFile(msg.content))
              case MsgType.DOC => SendDocument(chat_id, InputFile(msg.content))
            }
            request(content).void
          case None =>
            ().pure[F]
        }
      }

  //  def generateMessageGivenType(t: MsgType): F[Unit]

  override def receiveMessage(msg: Message): F[Unit] =
    for {
      _ <- Async[F].delay(logger.info(msg.toString))
      _ <- SQLCommands.insertMessage(msg).transact(xa)
      _ <- generateMessage(msg.chat.id)
    } yield ()

}

object Bot extends IOApp {

  import cats.effect._
  import doobie.{Transactor, _}
  import doobie.util.transactor._

  val botClientBackend: IO[SttpBackend[IO, Any]] =
    AsyncHttpClientCatsBackend[IO]()

  override def run(args: List[String]): IO[ExitCode] =
    for {
      token <- IO.fromOption(sys.env.get("TRASHTALK_TOKEN"))(
        new Exception(
          "Telegram Bot API token is not set. " +
            "Please set the \"TRASHTALK_TOKEN\" environment variable" +
            " with your token value."
        )
      )
      backend <- botClientBackend
      dummy = Transactor[IO, Unit](
        (),
        _ => Resource.pure(null),
        KleisliInterpreter[IO].ConnectionInterpreter,
        Strategy.void,
      )
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
