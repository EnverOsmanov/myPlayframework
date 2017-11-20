package yalp.sbt.run

import play.dev.filewatch.SourceModificationWatch
import sbt.internal.io.PlaySource
import sbt.{State, Watched}


private[run] trait PlayRunCompat {
  def sleepForPoolDelay = Thread.sleep(Watched.PollDelay.toMillis)

  def getPollInterval(watched: Watched): Int = watched.pollInterval.toMillis.toInt

  def getSourcesFinder(watched: Watched, state: State): SourceModificationWatch.PathFinder = () => {
    watched.watchSources(state)
      .map(source => new PlaySource(source))
      .flatMap(_.getFiles)
      .collect {
        case f if f.exists() => better.files.File(f.toPath)
      }(scala.collection.breakOut)
  }
}
