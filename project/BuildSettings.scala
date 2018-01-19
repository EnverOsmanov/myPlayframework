import sbt.{Compile, Project, Setting, Test, file}
import sbt.fileToRichFile
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}



object BuildSettings {

  def playScriptedSettings: Seq[Setting[_]] = Seq(
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },

    scriptedBufferLog := false
  )

  private def playRuntimeSettings: Seq[Setting[_]] = Seq(
    unmanagedSourceDirectories in Compile += {
      (sourceDirectory in Compile).value / s"scala-${scalaBinaryVersion.value}"
    }
  )

  private def playCommonSettings: Seq[Setting[_]] = {
    Seq(
      fork in Test := true
    )
  }

  def PlayNonCrossBuiltProject(name: String, dir: String): Project =
    Project(name, file(s"src/$dir"))
      .settings(playRuntimeSettings)
      .settings(
        crossPaths := false
      )

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
      .settings(playCommonSettings)
  }

  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
      .settings(sbtPlugin := true)
      .settings(playCommonSettings)
      .settings(playScriptedSettings)
  }

  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
      .settings(playRuntimeSettings: _*)
  }

}
