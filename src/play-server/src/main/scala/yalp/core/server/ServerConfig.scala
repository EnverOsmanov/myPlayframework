package yalp.core.server

import java.io.File
import java.util.Properties

import yalp.api.{Configuration, Mode}

case class ServerConfig(rootDir: File,
                        port: Option[Int],
                        sslPort: Option[Int],
                        address: String,
                        mode: Mode,
                        properties: Properties,
                        configuration: Configuration
                       ) {
  if (port.isEmpty && sslPort.isEmpty)
    throw new IllegalArgumentException("Must provide either an HTTP port or an HTTPS port")

}

object ServerConfig {

  def rootDirConfig(rootDir: File): Map[String, String] =
    Map("yalp.server.dir" -> rootDir.getAbsolutePath)

}
