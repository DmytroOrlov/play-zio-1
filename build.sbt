ThisBuild / organization := "com.example"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq(
  "-Ymacro-annotations",
)

val V = new {
  val distage = "0.10.19"
  val zio = "1.0.1"
  val silencer = "1.4.4"
  val betterMonadicFor = "0.3.1"
}

val Deps = new {
  val distageFramework = "io.7mind.izumi" %% "distage-framework" % V.distage
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
}

val commonSettings = Seq(
  scalaVersion := "2.13.3",
)

lazy val `play-zio-distage` = (project in file("."))
  .settings(commonSettings)
  .aggregate(macros, server, client, sharedJvm, sharedJs)

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

lazy val server = project
  .enablePlugins(PlayScala)
  .settings(commonSettings)
  .settings(
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      Deps.distageFramework,

      "org.iq80.leveldb" % "leveldb" % "0.12",
      "dev.zio" %% "zio" % V.zio,

      "com.vmunier" %% "scalajs-scripts" % "1.1.4",
    ),
    addCompilerPlugin(Deps.betterMonadicFor),
  )
  .dependsOn(sharedJvm, macros)

lazy val client = project
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "1.1.0",
      "dev.zio" %%% "zio" % "1.0.1",
    ),
  )
  .dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
  .jsConfigure(_.enablePlugins(ScalaJSWeb))
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
