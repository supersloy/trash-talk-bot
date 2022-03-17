import org.scalajs.jsenv.selenium.SeleniumJSEnv
import org.openqa.selenium.chrome.ChromeOptions

ThisBuild / scalaVersion               := "2.13.8"
ThisBuild / scapegoatVersion           := "1.4.12"
ThisBuild / version                    := "0.1.0-SNAPSHOT"
ThisBuild / semanticdbEnabled          := true
ThisBuild / semanticdbVersion          := scalafixSemanticdb.revision
ThisBuild / scalafixScalaBinaryVersion := "2.13"
ThisBuild / licenses                   := Seq(License.MIT)
scapegoatReports                       := Seq("xml")

val CatsVersion     = "3.3.5"
val Http4sVersion   = "0.23.10"
val CirceVersion    = "0.14.1"
val Log4CatsVersion = "2.2.0"
val DoobieVersion   = "1.0.0-RC1"

val slinkyVersion = "0.7.2"

def seleniumConfig(
  port: Int,
  baseDir: File,
  testJsDir: File,
): SeleniumJSEnv.Config = {
  import _root_.io.github.bonigarcia.wdm.WebDriverManager
  WebDriverManager.chromedriver().setup()
  SeleniumJSEnv
    .Config()
    .withMaterializeInServer(
      testJsDir.getAbsolutePath, {
        val path =
          s"http://localhost:$port/${testJsDir.relativeTo(baseDir).get}/"
        println(path)
        path
      },
    )
}

val githubWorkflowScalas = List("2.13.8")

val checkoutSetupJava = List(WorkflowStep.Checkout) ++
  WorkflowStep.SetupJava(List(JavaSpec.temurin("11")))

ThisBuild / githubWorkflowPublishTargetBranches := Seq()

ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Use(
    UseRef.Public("nanasess", "setup-chromedriver", "v1.0.1")
  ),
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v3.0.0"),
    params = Map(
      "node-version"          -> "lts/gallium",
      "cache"                 -> "npm",
      "cache-dependency-path" -> "frontend/package-lock.json",
    ),
  ),
)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Run(
    List(
      """cd frontend && \
         npm install && \
         { npx vite -l silent --clearScreen false & } && \
         cd ..  && \
         sbt ++${{ matrix.scala }} test && \
         kill $(jobs -p)"""
    )
  )
)

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id = "scalafmt",
    name = "Format code with scalafmt",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(
        WorkflowStep.Sbt(List("scalafmtCheckAll")),
        WorkflowStep.Sbt(List("scalafmtSbtCheck")),
      ),
  ),
  WorkflowJob(
    id = "scalafix",
    name = "Check code with scalafix",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(WorkflowStep.Sbt(List("scalafixAll --check"))),
  ),
  WorkflowJob(
    id = "coverage",
    name = "Upload coverage report to Codecov",
    scalas = githubWorkflowScalas,
    steps = checkoutSetupJava ++
      githubWorkflowGeneratedCacheSteps.value ++
      List(
        // TODO: For now, coverage is collected only from 'bot' subproject.
        //       Figure out how to setup sbt-scoverage with scala-js (probably unreal)
        WorkflowStep.Sbt(
          List("project bot", "coverage", "test", "coverageReport")
        ),
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

lazy val bot = project
  .in(file("bot"))
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

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withSourceMap(false)
    },
    Test / jsEnv := {
      new SeleniumJSEnv(
        new ChromeOptions().setHeadless(true),
        seleniumConfig(
          port = 3000,
          baseDir = baseDirectory.value,
          testJsDir = (Test / fastLinkJS / scalaJSLinkerOutputDirectory).value,
        ),
      )
    },
    libraryDependencies ++= Seq(
      "me.shadaj"     %%% "slinky-core" % slinkyVersion,
      "me.shadaj"     %%% "slinky-web"  % slinkyVersion,
      "me.shadaj"     %%% "slinky-hot"  % slinkyVersion,
      "org.scalameta" %%% "munit"       % "1.0.0-M2",
    ),
  )
