scalaVersion := "3.3.3"

val circeVersion  = "0.14.8"
val http4sVersion = "0.23.27"
val munitVersion  = "1.0.0"

libraryDependencies ++= List(
  "org.typelevel" %% "cats-core"               % "2.12.0",
  "org.http4s"    %% "http4s-circe"            % http4sVersion,
  "org.http4s"    %% "http4s-ember-server"     % http4sVersion,
  "org.http4s"    %% "http4s-ember-client"     % http4sVersion,
  "org.http4s"    %% "http4s-dsl"              % http4sVersion,
  "io.circe"      %% "circe-generic"           % circeVersion,
  "io.circe"      %% "circe-literal"           % circeVersion,
  "org.scalameta" %% "munit"                   % munitVersion % Test,
  "org.scalameta" %% "munit-scalacheck"        % munitVersion % Test,
  "org.typelevel" %% "munit-cats-effect"       % "2.0.0"      % Test,
  "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4"      % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Wunused:imports,privates,locals",
  "-explain-types",
  "-new-syntax",
  "-rewrite"
)

Compile / run / fork := true
