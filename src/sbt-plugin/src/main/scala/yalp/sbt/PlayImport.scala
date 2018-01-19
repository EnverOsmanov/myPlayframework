package yalp.sbt

import play.dev.filewatch.FileWatchService
import sbt.{File, SettingKey, TaskKey}

object PlayImport {

  object PlayKeys {
    val playDefaultPort = SettingKey[Int]("playDefaultPort", "The default port that Play runs on")
    val playDefaultAddress = SettingKey[String]("playDefaultAddress", "The default address that Play runs on")

    val externalizeResources = SettingKey[Boolean]("playExternalizeResources", "Whether resources should be externalized into the conf directory when Play is packaged as a distribution.")

    /** Our means of hooking the run task with additional behavior. */
    val playRunHooks = TaskKey[Seq[PlayRunHook]]("playRunHooks", "Hooks to run additional behaviour before/after the run task")

    /** A hook to configure how play blocks on user input while running. */
    val playInteractionMode = SettingKey[PlayInteractionMode]("playInteractionMode", "Hook to configure how Play blocks when running")
    val playExternalizedResources = TaskKey[Seq[(File, String)]]("playExternalizedResources", "The resources to externalize")
    val playJarSansExternalized = TaskKey[File]("playJarSansExternalized", "Creates a jar file that has all the externalized resources excluded")

    val playPlugin = SettingKey[Boolean]("playPlugin")

    val devSettings = SettingKey[Seq[(String, String)]]("playDevSettings")

    val assetsPrefix = SettingKey[String]("assetsPrefix")


    val playMonitoredFiles = TaskKey[Seq[File]]("playMonitoredFiles")
    val fileWatchService = SettingKey[FileWatchService]("fileWatchService", "The watch service Play uses to watch for file changes")

  }

}
