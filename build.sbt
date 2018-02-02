
name := "ehrchain"

version := "0.1"

scalaVersion := "2.12.3"

fork := true

lazy val scorexRootProject = RootProject(uri("https://github.com/greenhat/Scorex.git#master"))
//lazy val scorexProject = ProjectRef(uri("https://github.com/greenhat/Scorex.git#master")), "scorex")

lazy val root = (project in file(".")).dependsOn(scorexRootProject)
