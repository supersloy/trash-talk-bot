package org.trashtalk
import cats.effect._
import cats.syntax.all._
import org.trashtalk.persistence.repository.PostgresTelegramMessageRepository
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import doobie.util.transactor.Transactor
import org.trashtalk.bot.Bot

object Main extends IOApp {

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
      postgresRepo = PostgresTelegramMessageRepository(xa)
      bot <- IO.pure(new Bot(token, backend, postgresRepo))
      botResource = Resource.make(bot.run().start)(_ =>
        IO.blocking(bot.shutdown())
      )
      _ <- botResource.use(_ => IO.never)
    } yield ExitCode.Success

}
