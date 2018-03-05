
name := "ehrchain"

version := "4.0"

scalaVersion := "2.12.3"

fork := true

lazy val scorexRootProject = RootProject(uri("https://github.com/greenhat/Scorex.git#master"))
lazy val scorexTestkitProject = ProjectRef(uri("https://github.com/greenhat/Scorex.git#master"), "testkit")
lazy val scorexExamplesProject = ProjectRef(uri("https://github.com/greenhat/Scorex.git#master"), "examples")

lazy val root = (project in file("."))
  .dependsOn(scorexRootProject)
  .dependsOn(scorexTestkitProject)
  .dependsOn(scorexExamplesProject)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test",
)

wartremoverErrors ++= Warts.allBut(Wart.Equals, Wart.ImplicitParameter)

