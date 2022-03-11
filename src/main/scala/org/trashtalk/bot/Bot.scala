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

class Bot[F[_] : Async](token: String, backend: SttpBackend[F, Any], xa: Transactor[F])
  extends TelegramBot[F](token, backend) with
    Polling[F] {

  override def receiveMessage(msg: Message): F[Unit] = {
    println(msg)
    for {
      _ <- SQLCommands.insertMessage(msg).transact(xa)
      _ <- request(SendVideo(msg.source, FileId(msg.video.get.fileId))).void
//        request(SendMessage(msg.source, text.reverse)).void

    } yield ()

  }
}

object Bot extends IOApp {
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