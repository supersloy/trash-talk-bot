addSbtPlugin("org.jetbrains.scala"     % "sbt-ide-settings"   % "1.1.1")
addSbtPlugin("org.scalameta"           % "sbt-scalafmt"       % "2.4.6")
addSbtPlugin("org.scoverage"           % "sbt-scoverage"      % "1.9.3")
addSbtPlugin("com.sonar-scala"         % "sbt-sonar"          % "2.3.0")
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat"      % "1.1.1")
addSbtPlugin("ch.epfl.scala"           % "sbt-scalafix"       % "0.9.34")
addSbtPlugin("com.codecommit"          % "sbt-github-actions" % "0.14.2")

libraryDependencies += "io.github.bonigarcia" % "webdrivermanager"     % "5.0.3"
libraryDependencies += "org.scala-js"        %% "scalajs-env-selenium" % "1.1.1"
// note, 'sbt-scalajs' must come after 'scalajs-env-selenium'
// reference: https://github.com/scala-js/scala-js-env-selenium#usage
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.9.0")
