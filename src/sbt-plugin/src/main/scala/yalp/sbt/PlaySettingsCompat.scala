package yalp.sbt

import sbt.File
import sbt.Path._
import sbt.io.syntax._
import sbt.internal.inc.Analysis
import xsbti.compile.CompileAnalysis

import scala.concurrent.duration.Duration

private[sbt] trait PlaySettingsCompat {

  def getPoolInterval(poolInterval: Duration): Duration = poolInterval

  def getPlayCompileEverything(analysisSeq: Seq[CompileAnalysis]): Seq[Analysis] = {
    analysisSeq.map(_.asInstanceOf[Analysis])
  }

  def getPlayExternalizedResources(rdirs: Seq[File], unmanagedResourcesValue: Seq[File]): Seq[(File, String)] = {
    (unmanagedResourcesValue --- rdirs) pair (relativeTo(rdirs) | flat)
  }

}
