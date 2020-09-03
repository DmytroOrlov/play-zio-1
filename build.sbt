ThisBuild / organization := "fr.adelegue"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq(
  "-Ymacro-annotations",
)

val V = new {
  val zio = "1.0.1"
  val silencer = "1.4.4"
}

lazy val `play-zio-sample` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "org.iq80.leveldb" % "leveldb" % "0.12",
      "org.typelevel" %% "cats-effect" % "2.1.4",
      "dev.zio" %% "zio" % V.zio,
      "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  )
  .dependsOn(macros)
  .aggregate(macros)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "fr.adelegue.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "fr.adelegue.binders._"

lazy val macros = project
  .settings(
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % V.silencer cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % V.silencer % Provided cross CrossVersion.full,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "dev.zio" %% "zio-test-sbt" % V.zio % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions += "-language:experimental.macros",
  )
