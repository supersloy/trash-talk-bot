package org.trashtalk.bot
import cats._
import cats.effect._
import cats.implicits._
import doobie.util.transactor.Transactor
import org.http4s.circe._
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import doobie.implicits._


class Server[F[_]: Async](xa: Transactor[F]) {

  def routes: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / chatId =>
        chatId.toLongOption
          .map(id =>
            for {
              messages <- SQLCommands.getChatMessages(id).transact(xa)
              response <- Ok(messages.asJson)
            } yield response
          )
          .getOrElse(BadRequest(s"Chat with id \"$chatId\" does not exist"))

    }
  }

  def app: HttpApp[F] =
    Router(
      "/" -> routes
    ).orNotFound

}

object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    code <- IO(ExitCode.Success)
    transactor <- IO.delay(
      Transactor.fromDriverManager[IO](
        driver = "org.postgresql.Driver",
        url = "jdbc:postgresql://localhost:5432/postgres",
        user = "postgres",
        pass = "changeme",
      )
    )
    _ <- BlazeServerBuilder[IO]
      .bindHttp(port = 8080)
      .withHttpApp(new Server(transactor).app)
      .resource
      .use(_ => IO.never)
  } yield code

}
