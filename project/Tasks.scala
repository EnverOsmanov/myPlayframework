import sbt.File
import sbt.IO
import sbt.fileToRichFile
import sbt.Keys._
import sbt.complete.Parsers

object Tasks {

  def PlayVersion(
                   version: String,
                   scalaVersion: String,
                   sbtVersion: String,
                   jettyAlpnAgentVersion: String,
                   dir: File
                 ): Seq[File] = {

    val file = dir / "PlayVersion.scala"
    val scalaSource =
      """|package yalp.core
         |
         |object PlayVersion {
         |  val current = "%s"
         |  val scalaVersion = "%s"
         |  val sbtVersion = "%s"
         |  private[yalp] val jettyAlpnAgentVersion = "%s"
         |}
         |""".stripMargin.format(
        version,
        scalaVersion,
        sbtVersion,
        jettyAlpnAgentVersion
      )

    if (!file.exists || IO.read(file) != scalaSource) {
      IO.write(file, scalaSource)
    }

    Seq(file)
  }
}
