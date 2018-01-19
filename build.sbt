import BuildSettings.{PlayCrossBuiltProject, PlaySbtPluginProject, PlaySbtProject, PlayNonCrossBuiltProject}
import Dependencies.{ runtime, sbtDependencies, runSupportDependencies, streamsDependencies, jettyAlpnAgent, logback }
import Tasks.PlayVersion


name          := "myPlayframework"
version       := "0.1-SNAPSHOT"
scalaVersion  := "2.12.4"

organization in ThisBuild := "com.typesafe.yalp"

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
    libraryDependencies ++= sbtDependencies((sbtVersion in pluginCrossBuild).value, scalaVersion.value),
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

lazy val PlayLogback = PlayCrossBuiltProject("Play-Logback", "play-logback")
  .settings(libraryDependencies += logback)
  .dependsOn(PlayProject)

lazy val PlayExceptionsProject = PlayNonCrossBuiltProject("Play-Exceptions", "play-exceptions")

lazy val PlayProject = PlayCrossBuiltProject("Play", "play")
  .settings(
    libraryDependencies ++= runtime,
    sourceGenerators in Compile += Def.task(PlayVersion(
      version.value,
      scalaVersion.value,
      sbtVersion.value,
      jettyAlpnAgent.revision,
      (sourceManaged in Compile).value
    )),
    sourceDirectories in Compile := (unmanagedSourceDirectories in Compile).value
  )
  .dependsOn(BuildLinkProject, StreamsProject)

lazy val PlayServerProject = PlayCrossBuiltProject("Play-Server", "play-server")
  .dependsOn(PlayProject)

lazy val StreamsProject = PlayCrossBuiltProject("Play-Streams", "play-streams")
  .settings(libraryDependencies ++= streamsDependencies)