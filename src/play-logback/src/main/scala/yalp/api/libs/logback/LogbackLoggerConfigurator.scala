package yalp.api.libs.logback

import java.io.File
import java.net.URL

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.ILoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.impl.StaticLoggerBinder
import yalp.api._

class LogbackLoggerConfigurator extends LoggerConfigurator {

  override def loggerFactory: ILoggerFactory = {
    StaticLoggerBinder.getSingleton.getLoggerFactory
  }

  override def init(rootPath: File, mode: Mode): Unit = {
    Logger.setApplicationMode(mode)

    val properties = Map("application.home" -> rootPath.getAbsolutePath)
    val resourceName = if (mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"
    val resourceUrl = Option(this.getClass.getClassLoader.getResource(resourceName))
    configure(properties, resourceUrl)
  }

  override def configure(env: Environment): Unit = {
    configure(env, Configuration.empty, Map.empty)
  }

  override def configure(env: Environment, configuration: Configuration, optionalProperties: Map[String, String]): Unit = {

    def explicitResourceUrl = sys.props.get("logger.resource").map { r =>
      env.resource(r).getOrElse(new File(env.getFile("conf"), r).toURI.toURL)
    }

    def explicitFileUrl = {
      sys.props.get("logger.file")
        .map(new File(_).toURI.toURL)
    }

    def explicitUrl = sys.props.get("logger.url").map(new URL(_))

    def resourceUrl = {
      def resourceName = if (env.mode == Mode.Dev) "logback-play-dev.xml" else "logback-play-default.xml"

      env.resource("logback.xml") orElse env.resource(resourceName)
    }

    val configUrl = explicitResourceUrl orElse explicitFileUrl orElse explicitUrl orElse resourceUrl
    val properties = LoggerConfigurator.generateProperties(env, configuration, optionalProperties)

    configure(properties, configUrl)
  }

  override def configure(properties: Map[String, String], config: Option[URL]): Unit = {

    loggerFactory.synchronized {
      SLF4JBridgeHandler.removeHandlersForRootLogger()

      val ctx = loggerFactory.asInstanceOf[LoggerContext]

      val configurator = new JoranConfigurator
      configurator.setContext(ctx)

      val levelChangePropagator = new LevelChangePropagator()
      levelChangePropagator.setContext(ctx)
      levelChangePropagator.setResetJUL(true)
      ctx.addListener(levelChangePropagator)
      SLF4JBridgeHandler.install()

      ctx.reset()

      val frameworkPackages = ctx.getFrameworkPackages
      frameworkPackages.add(classOf[yalp.api.Logger].getName)

      properties.foreach{ case (k, v) => ctx.putProperty(k, v) }

      config match {
        case Some(url) => configurator.doConfigure(url)
        case None => System.err.println("Could not detect a logback configuration file, not configuring logback")
      }

      StatusPrinter.printIfErrorsOccured(ctx)
    }
  }

}
