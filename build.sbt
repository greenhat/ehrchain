
name := "ehrchain"

version := "8.0"

scalaVersion := "2.12.3"

fork := true

lazy val scorexRootProject = RootProject(uri("https://github.com/greenhat/Scorex.git#deploy"))
lazy val scorexTestkitProject = ProjectRef(uri("https://github.com/greenhat/Scorex.git#deploy"), "testkit")
lazy val scorexExamplesProject = ProjectRef(uri("https://github.com/greenhat/Scorex.git#deploy"), "examples")

lazy val root = (project in file("."))
  .dependsOn(scorexRootProject)
  .dependsOn(scorexTestkitProject)
  .dependsOn(scorexExamplesProject)

lazy val akkaVersion = "2.5.+"
lazy val akkaHttpVersion = "10.1.+"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "org.scalactic" %% "scalactic" % "3.0.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
  "com.typesafe.akka" %% "akka-testkit-typed" % akkaVersion % "test"
)

wartremoverErrors ++= Warts.allBut(Wart.Equals, Wart.ImplicitParameter, Wart.Product, Wart.Serializable)

