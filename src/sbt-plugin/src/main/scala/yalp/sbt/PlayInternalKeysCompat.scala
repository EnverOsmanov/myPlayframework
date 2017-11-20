package yalp.sbt

import sbt.TaskKey
import sbt.internal.inc.Analysis

trait PlayInternalKeysCompat {
  val playReload = TaskKey[Analysis]("playReload", "Executed when sources of changed, to recompile (and possibly reload) the app")
  val playCompileEverything = TaskKey[Seq[Analysis]]("playCompileEverything", "Compiles this project and every project it depends on.")

}
