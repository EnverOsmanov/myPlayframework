
package sbt.internal.io {

  import java.io.File

  class PlaySource(source: sbt.internal.io.Source) {
    def getFiles: Seq[File] = {
      source.getUnfilteredPaths
        .filter(p => source.accept(p))
        .map(_.toFile)
    }
  }
}
