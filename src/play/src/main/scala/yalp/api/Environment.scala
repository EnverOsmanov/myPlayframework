package yalp.api

import java.io.File
import java.net.URL


case class Environment(
                        rootPath: File,
                        classLoader: ClassLoader,
                        mode: Mode
                      ) {

  def getFile(relativePath: String): File = new File(rootPath, relativePath)

  def resource(name: String): Option[URL] = {
    val n = name.stripPrefix("/")
    Option(classLoader.getResource(n))
  }

}

object Environment {

}
