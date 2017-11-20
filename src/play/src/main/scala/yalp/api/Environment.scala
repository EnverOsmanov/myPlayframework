package yalp.api

import java.io.File


case class Environment(
                        rootPath: File,
                        classLoader: ClassLoader,
                        mode: Mode
                      )

object Environment {

}
