package yalp.core.server

import java.util.concurrent.locks.StampedLock

import yalp.api.inject.DefaultApplicationLifecycle
import yalp.api.{Application, Configuration}
import yalp.api.mvc.{RequestHeader, Result}
import yalp.core.ApplicationProvider
import yalp.core.system.WebCommands

import scala.concurrent.Future
import scala.util.Try

trait ServerProvider {
  def createServer(context: ServerProvider.Context): Server
}

object ServerProvider {

  def fromConfiguration(classLoader: ClassLoader, configuration: Configuration): ServerProvider = {
    val ClassNameConfigKey = "yalp.server.provider"

    val className = configuration
      .getOptional[String](ClassNameConfigKey)
      .getOrElse(throw ServerStartException(s"No ServerProvider configured with key '$ClassNameConfigKey'"))

    val clazz =
      try classLoader.loadClass(className)
      catch { case _: ClassNotFoundException => throw ServerStartException(s"Couldn't find ServerProvider class '$className'")}

    if (!classOf[ServerProvider].isAssignableFrom(clazz)) throw ServerStartException(s"Class ${clazz.getName} must implement ServerProvider interface")

    val ctor =
      try clazz.getConstructor()
      catch { case _: NoSuchMethodException => throw ServerStartException(s"ServerProvider class ${clazz.getName} must have a public default constructor")}

    ctor.newInstance().asInstanceOf[ServerProvider]
  }

  final case class Context(
                            serverConfig: ServerConfig,
                            appProvider: ApplicationProvider,
                            stopHook: () => Future[_])

}
