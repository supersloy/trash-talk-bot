ThisBuild / scalaVersion               := "2.13.8"
ThisBuild / scapegoatVersion           := "1.4.12"
ThisBuild / semanticdbEnabled          := true
ThisBuild / semanticdbVersion          := scalafixSemanticdb.revision
ThisBuild / scalafixScalaBinaryVersion := "2.13"
scapegoatReports                       := Seq("xml")

val CatsVersion     = "3.3.5"
val Http4sVersion   = "0.23.10"
val CirceVersion    = "0.14.1"
val Log4CatsVersion = "2.2.0"
val DoobieVersion   = "1.0.0-RC1"

val githubWorkflowScalas = List("2.13.8")

val checkoutSetupJava = List(WorkflowStep.Checkout) ++
  WorkflowStep.SetupJava(List(JavaSpec.temurin("11")))

ThisBuild / githubWorkflowPublishTargetBranches := Seq()

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id = "scalafmt",
    name = "Format code with scalafmt",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(WorkflowStep.Sbt(List("scalafmtCheckAll"))),
  ),
  WorkflowJob(
    id = "scalafix",
    name = "Check code with scalafix",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(WorkflowStep.Sbt(List("scalafixAll", "--check"))),
  ),
  WorkflowJob(
    id = "coverage",
    name = "Upload coverage report to Codecov",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(
        WorkflowStep.Sbt(List("coverage", "test", "coverageReport")),
        WorkflowStep.Run(
          List(
            "curl -Os https://uploader.codecov.io/latest/linux/codecov",
            "chmod +x codecov",
            "./codecov",
          )
        ),
      ),
  ),
)

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
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.4.2",
      "org.scalameta" %% "munit" % "1.0.0-M2" % Test,
    ),
  )
