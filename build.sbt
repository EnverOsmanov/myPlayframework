import BuildSettings.{PlayCrossBuiltProject, PlaySbtPluginProject, PlaySbtProject, PlayNonCrossBuiltProject}
import Dependencies.{ runtime, sbtDependencies, runSupportDependencies, streamsDependencies, jettyAlpnAgent }
import Tasks.PlayVersion


organization in ThisBuild := "com.typesafe.yalp"
name := "myPlayframework"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

lazy val BuildLinkProject = PlayNonCrossBuiltProject("Build-Link", "build-link")
  .dependsOn(PlayExceptionsProject)

lazy val RunSupportProject = PlaySbtProject("Run-Support", "run-support")
  .settings(
    target := target.value / "run-support",
    libraryDependencies ++= runSupportDependencies
  )
  .dependsOn(BuildLinkProject)

lazy val SbtPluginProject = PlaySbtPluginProject("SBT-Plugin", "sbt-plugin")
  .settings(
    libraryDependencies ++= sbtDependencies,
    sourceGenerators in Compile += Def.task(PlayVersion(
      version.value,
      (scalaVersion in PlayProject).value,
      sbtVersion.value,
      jettyAlpnAgent.revision,
      (sourceManaged in Compile).value
    )),
    scriptedDependencies := {
      val () = publishLocal.value
    }
  )
  .dependsOn(RunSupportProject)

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "play-exceptions")

lazy val PlayProject = PlayCrossBuiltProject("Play", "play")
  .settings(
    libraryDependencies ++= runtime
  )
  .dependsOn(BuildLinkProject, StreamsProject)

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "play-server")
  .dependsOn(PlayProject)

lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "play-streams")
  .settings(libraryDependencies ++= streamsDependencies)