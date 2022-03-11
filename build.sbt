ThisBuild / scalaVersion := "2.13.8"

val CatsVersion     = "3.3.5"
val Http4sVersion   = "0.23.9"
val CirceVersion    = "0.14.1"
val Log4CatsVersion = "2.2.0"
val DoobieVersion   = "1.0.0-RC1"

lazy val trashtalkBot = project
  .in(file("."))
  .settings(Compiler.settings)
  .settings(
    name := "Trash Talk Telegram Bot",
    libraryDependencies ++= Seq(
      "com.bot4s"     %% "telegram-core"       % "5.3.0",
      "org.tpolecat"  %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"  %% "doobie-postgres"     % DoobieVersion,
      "org.tpolecat"  %% "doobie-hikari"       % DoobieVersion,
      "org.http4s"    %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"    %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"    %% "http4s-circe"        % Http4sVersion,
      "org.typelevel" %% "cats-effect"         % CatsVersion,
      "org.typelevel" %% "log4cats-slf4j"      % Log4CatsVersion,
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.4.1",
    ).map(_.cross(CrossVersion.for3Use2_13)),
  )
