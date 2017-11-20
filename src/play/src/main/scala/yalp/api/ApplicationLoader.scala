package yalp.api

import yalp.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle}
import yalp.core.SourceMapper
import yalp.utils.Reflect

trait ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application
}

object ApplicationLoader {

  final case class Context(
                            environment: Environment, sourceMapper: Option[SourceMapper],
                            initialConfiguration: Configuration, lifecycle: ApplicationLifecycle
                          )

  private[yalp] final class NoApplicationLoader extends ApplicationLoader {
    override def load(context: Context): Application = loaderNotFound()
  }

  def createContext(
                     environment: Environment,
                     sourceMapper: Option[SourceMapper] = None,
                     initialSettings: Map[String, AnyRef] = Map.empty,
                     lifecycle: ApplicationLifecycle = new DefaultApplicationLifecycle
                   ): Context = {
    val configuration = Configuration.load(environment, initialSettings)
    Context(environment, sourceMapper, configuration, lifecycle)
  }

  def apply(context: Context): ApplicationLoader = {
    val loaderKey = "yalp.application.loader"

    if (!context.initialConfiguration.has(loaderKey)) {
      loaderNotFound()
    }

    Reflect.configuredClass[ApplicationLoader, NoApplicationLoader](
      context.environment, context.initialConfiguration, loaderKey, classOf[NoApplicationLoader].getName
    ) match {
      case None => loaderNotFound()

      case Some(scalaClass) => scalaClass.newInstance
    }
  }

  // Method to call if we cannot find a configured ApplicationLoader
  private def loaderNotFound(): Nothing = {
    sys.error("No application loader is configured. Please configure an application loader either using the " +
      "play.application.loader configuration property, or by depending on a module that configures one. " +
      "You can add the Guice support module by adding \"libraryDependencies += guice\" to your build.sbt.")
  }

}
