import sbt.{Project, file, Setting, Compile}
import sbt.fileToRichFile
import sbt.Keys.{scalaBinaryVersion, sourceDirectory, sbtPlugin, version, unmanagedSourceDirectories}
import sbt.ScriptedPlugin.autoImport.{scriptedLaunchOpts, scriptedBufferLog}



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
    },
  )

  def PlayNonCrossBuiltProject(name: String, dir: String): Project =
    Project(name, file(s"src/$dir"))

  def PlaySbtProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
  }

  def PlaySbtPluginProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
      .settings(sbtPlugin := true)
      .settings(playScriptedSettings)
  }

  def PlayCrossBuiltProject(name: String, dir: String): Project = {
    Project(name, file(s"src/$dir"))
      .settings(playRuntimeSettings: _*)
  }

}
