package yalp.sbt

import sbt.Def.Classpath
import sbt.{File, TaskKey}

object PlayInternalKeys extends PlayInternalKeysCompat {
  val playDependencyClasspath = TaskKey[Classpath]("playDependencyClasspath", "The classpath containing all the jar dependencies of the project")
  val playReloaderClasspath = TaskKey[Classpath]("playReloaderClasspath", "The application classpath, containing all projects in this build that are dependencies of this project, including this project")

  val playCommonClassloader = TaskKey[ClassLoader]("playCommonClassloader")

  val playAllAssets = TaskKey[Seq[(String, File)]]("playAllAssets", "Compiles all assets for all projects")

  val playAssetsClassLoader = TaskKey[ClassLoader => ClassLoader]("playAssetsClassloader", "Function that creates a classloader from a given parent that contains all the assets.")
  val playPrefixAndAssets = TaskKey[(String, File)]("playPrefixAndAssets", "Gets all the assets with their associated prefixes")

}
