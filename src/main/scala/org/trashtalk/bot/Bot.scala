package org.trashtalk.bot

import cats.effect.*
import cats.syntax.functor.*
import com.bot4s.telegram.models.*
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.cats.TelegramBot
import com.bot4s.telegram.methods.*
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.SttpBackend

class Bot[F[_]: Async](token: String, backend: SttpBackend[F, Any])
    extends TelegramBot[F](token, backend),
      Polling[F]:

  override def receiveMessage(msg: Message): F[Unit] =
    msg.text.fold(unit) { text =>
      request(SendMessage(msg.source, text.reverse)).void
    }

object Bot extends IOApp:
  val botClientBackend: IO[SttpBackend[IO, Any]] =
    AsyncHttpClientCatsBackend[IO]()

  override def run(args: List[String]): IO[ExitCode] =
    for
      token <- IO.fromOption(sys.env.get("TRASHTALK_TOKEN"))(
        new Exception(
          "Telegram Bot API token is not set. " +
            "Please set the \"TRASHTALK_TOKEN\" environment variable" +
            " with your token value."
        )
      )
      backend <- botClientBackend
      bot     <- IO.pure(new Bot(token, backend))
      botResource = Resource.make(bot.run().start)(_ =>
        IO.blocking(bot.shutdown),
      )
      _ <- botResource.use(_ => IO.never)
    yield ExitCode.Success
