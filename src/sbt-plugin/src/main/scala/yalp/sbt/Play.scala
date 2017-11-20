package yalp.sbt

import sbt.{AutoPlugin, Plugins, Setting}

object Play extends AutoPlugin {

  override def requires: Plugins =
    super.requires

  override def projectSettings: Seq[Setting[_]] =
    PlaySettings.defaultSettings

}
