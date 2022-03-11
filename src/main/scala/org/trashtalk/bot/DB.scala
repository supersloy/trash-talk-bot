package org.trashtalk.bot

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.syntax.all._
import com.bot4s.telegram.models.Message
import org.trashtalk.bot.Schemas.MsgType.STICKER
import shapeless.{::, HNil}
import Fragments.values

object Schemas {

  sealed trait MsgType

  object MsgType {
    case object TEXT extends MsgType

    case object IMAGE extends MsgType

    case object STICKER extends MsgType

    case object GIF extends MsgType
  }


  import MsgType._

  val msg_table_name = "message"
  val msg_types = List(TEXT, IMAGE, STICKER, GIF)

  val chat =
    sql"""
       CREATE TABLE "chat"(
           "chat_id" bigint PRIMARY KEY
       );
       """

  val message =
    fr"""CREATE TYPE msg_type AS ENUM""" ++
      Fragment.const(msg_types.map(t => s"'$t'").mkString("(", ", ", ")")) ++
      fr""";
          |CREATE TABLE """.stripMargin ++ Fragment.const(msg_table_name) ++
      fr"""(
          |"chat_id" bigint REFERENCES "chat"("chat_id"),
          |"message_id" bigint NOT NULL,
          |"content" text NOT NULL,
          |"type" msg_type NOT NULL,
          |PRIMARY KEY (message_id, chat_id)
          |);
      """.stripMargin

  val admin_relation =
    sql"""
        |CREATE TABLE "admin_relation" (
        |"chat_id" bigint NOT NULL,
        |"admin_id" bigint NOT NULL,
        | FOREIGN KEY (chat_id) REFERENCES "chat"("chat_id"),
        | FOREIGN KEY (admin_id) REFERENCES "admin"("user_id")
        |);
         """.stripMargin

  val admin: Fragment =
    sql"""
      |CREATE TABLE "admin" (
      |  "user_id" bigint NOT NULL,
      |  PRIMARY KEY ("user_id")
      |);
         """.stripMargin
}

object SQLCommands {

  import org.trashtalk.bot.Schemas._

  case class DBMessage(chatId: Long, messageId: Long, msgType: MsgType, content: String)

  object DBMessage {
    def from(msg: Message): Option[DBMessage] = {
      ((msg.text, msg.sticker) match {
        case (Some(_), Some(_)) => None
        case (Some(text), None) => Some((MsgType.TEXT, text))
        case (None, Some(sticker)) => Some((MsgType.STICKER, sticker.fileId))
        case (None, None) => None
      }).map {
        case (msgType, content) => DBMessage(msg.chat.id, msg.messageId, msgType, content)
      }
    }
  }

  val reCreateSchema: ConnectionIO[Int] =
    sql"""
      DROP SCHEMA public CASCADE;
      CREATE SCHEMA public;
       """.update.run

  val createTables: ConnectionIO[Int] = (
    chat ++
    message
    ++ admin ++ admin_relation
    ).update.run

  // https://tpolecat.github.io/doobie/docs/07-Updating.html
  def insertMessage(msg: Message): ConnectionIO[Unit] = {
    DBMessage.from(msg).map(m => {
      sql"""
           INSERT INTO chat (chat_id) values (${m.chatId}) ON CONFLICT DO NOTHING;
         """.update.run*>
      sql"""insert into message (message_id, chat_id, content, type) values (${m.messageId}, ${m.chatId}, ${m.content}, ${m.msgType.toString}::msg_type);"""
        .update.run.void
    }
    ).getOrElse(
      cats.Applicative[ConnectionIO].unit
    )

  }
}

object DBConnect {
  def main(args: Array[String]): Unit = {
    import cats.effect.IO
    import cats.effect.unsafe.implicits.global
    import SQLCommands._

    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/postgres",
      user = "postgres",
      pass = "changeme"
    )

    (reCreateSchema *> createTables)
      .transact(transactor).unsafeRunSync()
  }

}
