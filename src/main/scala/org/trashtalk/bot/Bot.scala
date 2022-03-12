package org.trashtalk.bot

import cats.effect._
import cats.syntax.functor._
import com.bot4s.telegram.models._
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods._
import doobie.util.transactor.Transactor
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.SttpBackend
import doobie.implicits._
import cats.syntax.flatMap._
import com.bot4s.telegram.models.InputFile.FileId
import org.trashtalk.bot.Schemas.MsgType
import SQLCommands.DBMessage
import com.bot4s.telegram.api.declarative.{Action, CommandFilterMagnet, Commands}
import com.typesafe.scalalogging.Logger




class Bot[F[_] : Async](token: String, backend: SttpBackend[F, Any], xa: Transactor[F])
  extends TelegramBot[F](token, backend)
    with Polling[F]
    with Commands[F]
    {


  override val logger: Logger = ???


  override def onCommand(filter: CommandFilterMagnet)(action: Action[F, Message]): Unit = super.onCommand(filter)(action)

  def generateMessage(chat_id : Long): F[Unit] = {
    SQLCommands
      .getRandomMessage(chat_id)
      .transact(xa)
      .flatMap(list => {
        list.headOption match {
          case Some(msg) =>
            val content = msg.msgType match {
              case MsgType.TEXT => SendMessage(chat_id, msg.content)
              case MsgType.IMAGE => SendPhoto(chat_id, FileId(msg.content))
              case MsgType.STICKER => SendSticker(chat_id, FileId(msg.content))
              case MsgType.GIF => SendVideo(chat_id, FileId(msg.content))
            }
            request(content).void
          case None =>
            cats.Applicative[F].unit
        }
      })
  }

//  def generateMessageGivenType(t: MsgType): F[Unit]

  override def receiveMessage(msg: Message): F[Unit] = {
    for {
      _ <- SQLCommands.insertMessage(msg).transact(xa)
      _ <- generateMessage(msg.chat.id)
    } yield ()

  }
}

object Bot extends IOApp {
  import doobie._
  import doobie.Transactor
  import doobie.util.transactor._
  import cats._
  import cats.data._
  import cats.effect._
  import cats.implicits._
  import scala.concurrent._


  val botClientBackend: IO[SttpBackend[IO, Any]] =
    AsyncHttpClientCatsBackend[IO]()

  override def run(args: List[String]): IO[ExitCode] =
    for {
      token

        <- IO.fromOption(sys.env.get("TRASHTALK_TOKEN"))(
        new Exception(
          "Telegram Bot API token is not set. " +
            "Please set the \"TRASHTALK_TOKEN\" environment variable" +
            " with your token value."
        )
      )
      backend <- botClientBackend
//      dummy = Transactor(
//        (),
//        (_: Unit) => Resource[IO, java.sql.Connection].pure(null),
//        KleisliInterpreter[IO](
//          WeakAsyncConnectionIO[IO].liftExecutionContext()
//        ).ConnectionInterpreter,
//        Strategy.void
//      )
      xa <- IO.delay(Transactor.fromDriverManager[IO](
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost:5432/postgres",
        user = "postgres",
        pass = "changeme"
      ))
      bot <- IO.pure(new Bot(token, backend, xa))
      botResource = Resource.make(bot.run().start)(_ =>
        IO.blocking(bot.shutdown()),
      )
      _ <- botResource.use(_ => IO.never)
    }
    yield ExitCode.Success

}