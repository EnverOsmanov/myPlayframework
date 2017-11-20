lazy val root = (project in file("."))
  .enablePlugins(Play)
  .settings(
  version := "0.1",
  scalaVersion := "2.12.4"
  //any other config you need here
)