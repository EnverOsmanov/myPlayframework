package yalp.sbt

import sbt.internal.inc.Analysis
import xsbti.compile.CompileAnalysis

import scala.concurrent.duration.Duration

private[sbt] trait PlaySettingsCompat {

  def getPoolInterval(poolInterval: Duration): Duration = poolInterval

  def getPlayCompileEverything(analysisSeq: Seq[CompileAnalysis]): Seq[Analysis] = {
    analysisSeq.map(_.asInstanceOf[Analysis])
  }
}
