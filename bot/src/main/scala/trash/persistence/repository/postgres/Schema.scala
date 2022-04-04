package trash.persistence.repository.postgres

import doobie._
import doobie.implicits._
import cats.syntax.all._
import trash.persistence.model.MsgType

object Schema {

  val msgTableName: String = "message"

  val chat: Fragment =
    sql"""
       CREATE TABLE "chat"(
           "chat_id" bigint PRIMARY KEY
       );
       """

  val message: Fragment =
    fr"""CREATE TYPE msg_type AS ENUM""" ++
      Fragment.const(
        MsgType.msgTypes.map(t => s"'$t'").mkString("(", ", ", ")")
      ) ++
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

  def main(args: Array[String]): Unit = {

    import cats.effect.IO
    import cats.effect.unsafe.implicits.global

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
