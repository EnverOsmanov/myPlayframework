package yalp.sbt

import play.dev.filewatch.FileWatchService
import sbt.Keys._
import sbt.{Classpaths, Compile, Def, Defaults, Keys, Provided, Runtime, Setting, inConfig, stringToOrganization}
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import yalp.core.PlayVersion
import yalp.sbt.PlayImport.PlayKeys._
import yalp.sbt.PlayInternalKeys.{playCommonClassloader, playCompileEverything, playDependencyClasspath, playReloaderClasspath}
import yalp.sbt.run.{PlayRun, toLoggerProxy}

object PlaySettings extends PlaySettingsCompat {

  lazy val defaultSettings: Seq[Setting[_]] = Seq(
    playPlugin := false,

    externalizeResources := true,

    libraryDependencies += {
      if (playPlugin.value) "com.typesafe.yalp" %% "play" % PlayVersion.current % Provided
      else "com.typesafe.yalp" %% "play-server" % PlayVersion.current
    },

    playDependencyClasspath := (externalDependencyClasspath in Runtime).value,

    playReloaderClasspath := Classpaths.concatDistinct(exportedProducts in Runtime, internalDependencyClasspath in Runtime).value,
    playCommonClassloader := PlayCommands.playCommonClassloaderTask.value,
/*
    playCompileEverything := getPlayCompileEverything(PlayCommands.playCompileEverythingTask.value),
*/

    playMonitoredFiles := PlayCommands.playMonitoredFilesTask.value,

    fileWatchService := FileWatchService.defaultWatchService(target.value, getPoolInterval(pollInterval.value).toMillis.toInt, sLog.value),

    playDefaultPort := 9000,
    playDefaultAddress := "0.0.0.0",


    playRunHooks := Nil,
    devSettings := Nil,

    playInteractionMode := PlayConsoleInteractionMode,

    PlayRun.playAssetsClassLoaderSetting,
    PlayRun.playAllAssetsSetting,

    Keys.run in Compile := PlayRun.playDefaultRunTask.evaluated,
    mainClass in (Compile, Keys.run) := Some("yalp.core.server.DevServerStart"),

    //mainClass in Compile := Some("yalp.core.server.ProdServerStart"),

/*    mappings in Universal ++= {
      val resourceMappings = (playExternalizedResources in Compile).value

      if (externalizeResources.value) {
        resourceMappings.map {
          case (resource, path) => resource -> s"conf/$path"
        }
      }
      else Nil
    },

    mappings in Universal ++= Def.taskDyn {
      Def.task(Seq.empty[(sbt.File, String)])
    }.value*/
  ) ++ inConfig(Compile)(externalizedSettings)


  private def externalizedSettings: Seq[Setting[_]] = Seq(
    playExternalizedResources := getPlayExternalizedResources(
      unmanagedResourceDirectories.value,
      unmanagedResources.value
    )
  )/* ++ Defaults.packageTaskSettings(playJarSansExternalized, mappings in playJarSansExternalized)*/

}
