package org.trashtalk.bot

import cats.syntax.all._
import com.bot4s.telegram.models.Message
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import scala.collection.immutable.SortedMap

object Schemas {

  import MsgType._

  sealed trait MsgType

  object MsgType {
    final case object TEXT extends MsgType

    final case object IMAGE extends MsgType

    final case object STICKER extends MsgType

    final case object VIDEO extends MsgType

    final case object DOC extends MsgType

    private val mapping = SortedMap(
      "TEXT"    -> TEXT,
      "IMAGE"   -> IMAGE,
      "STICKER" -> STICKER,
      "VIDEO"   -> VIDEO,
      "DOC"     -> DOC,
    )

    val msgTypes: List[MsgType]    = mapping.values.toList
    def toEnum(e: MsgType): String = e.toString

    def fromEnum(s: String): Option[MsgType] = mapping.get(s)
    implicit val msgTypeMeta: Meta[MsgType] =
      pgEnumStringOpt("msg_type", MsgType.fromEnum, MsgType.toEnum)
  }

  val msgTableName: String = "message"

  val chat: Fragment =
    sql"""
       CREATE TABLE "chat"(
           "chat_id" bigint PRIMARY KEY
       );
       """

  val message: Fragment =
    fr"""CREATE TYPE msg_type AS ENUM""" ++
      Fragment.const(msgTypes.map(t => s"'$t'").mkString("(", ", ", ")")) ++
      fr""";
          |CREATE TABLE """.stripMargin ++ Fragment.const(msgTableName) ++
      fr"""(
          |"chat_id" bigint REFERENCES "chat"("chat_id"),
          |"message_id" bigint NOT NULL,
          |"type" msg_type NOT NULL,
          |"content" text NOT NULL,
          |PRIMARY KEY (message_id, chat_id)
          |);
      """.stripMargin

  val adminRelation: Fragment =
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

  final case class DBMessage(
    chatId: Long,
    messageId: Long,
    msgType: MsgType,
    content: String,
  )

  object DBMessage {
    def from(msg: Message): Option[DBMessage] =
      ((msg.text, msg.sticker, msg.video, msg.photo, msg.document) match {
        case (Some(text), None, None, None, None) => Some((MsgType.TEXT, text))
        case (None, Some(sticker), None, None, None) =>
          Some((MsgType.STICKER, sticker.fileId))
        case (None, None, Some(video), None, None) =>
          Some((MsgType.VIDEO, video.fileId))
        case (None, None, None, Some(photo +: _), None) =>
          Some((MsgType.IMAGE, photo.fileId))
        case (None, None, None, None, Some(document)) =>
          Some((MsgType.DOC, document.fileId))
        case _ => None
      }).map { case (msgType, content) =>
        DBMessage(msg.chat.id, msg.messageId, msgType, content)
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
      ++ admin ++ adminRelation
  ).update.run

  def getChatMessages(chatId: Long): ConnectionIO[List[DBMessage]] =
    sql"""
      SELECT chat_id, message_id, type, content FROM message
      WHERE chat_id = $chatId
       """.query[DBMessage].to[List]

  def getRandomMessage(chatId: Long): ConnectionIO[List[DBMessage]] =
    sql"""
      SELECT chat_id, message_id, type, content FROM message
      WHERE chat_id = $chatId
      ORDER BY RANDOM()
      LIMIT 1
       """.query[DBMessage].to[List]

  def insertMessage(msg: Message): ConnectionIO[Unit] =
    DBMessage
      .from(msg)
      .map { m =>
        sql"""
           INSERT INTO chat (chat_id) values (${m.chatId}) ON CONFLICT DO NOTHING;
         """.update.run *>
          sql"""insert into message (message_id, chat_id, content, type) values (${m.messageId}, ${m.chatId}, ${m.content}, ${m.msgType.toString}::msg_type);""".update.run.void
      }
      .getOrElse(
        cats.Applicative[ConnectionIO].unit
      )

}

object DBConnect {
  import SQLCommands._
  import cats.effect.IO
  import cats.effect.unsafe.implicits.global

  def main(args: Array[String]): Unit = {

    val transactor = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/postgres",
      user = "postgres",
      pass = "changeme",
    )

    (reCreateSchema *> createTables)
      .transact(transactor)
      .unsafeRunSync()
  }

}
