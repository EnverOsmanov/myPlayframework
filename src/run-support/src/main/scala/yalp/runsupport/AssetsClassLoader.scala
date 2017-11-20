package yalp.runsupport

import java.io.File
import java.net.URL

class AssetsClassLoader(parent: ClassLoader, assets: Seq[(String, File)]) extends ClassLoader(parent) {

  override def findResource(name: String): URL = assets.collectFirst {
    case (prefix, dir) if exists(name, prefix, dir) =>
      new File(dir, name.substring(prefix.length)).toURI.toURL
  }.orNull

  private def exists(name: String, prefix: String, file: File) = {
    name.startsWith(prefix) &&
      new File(file, name.substring(prefix.length)).isFile
  }

}
