package yalp.api

import java.io.File
import java.util.Properties

import com.typesafe.config._
import com.typesafe.config.impl.ConfigImpl
import yalp.api
import yalp.utils.PlayIO

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

case class Configuration(underlying: Config) {

  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      if (underlying.hasPathOrNull(path)) Some(v)
      else None
    }
    catch { case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))}
  }

  def getOptional[A](path: String)
                    (implicit loader: ConfigLoader[A]): Option[A] = {
    readValue(path, get[A](path))
  }


  def get[A](path: String)
            (implicit loader: ConfigLoader[A]) =
    loader.load(underlying, path)


  def has(path: String): Boolean = underlying.hasPath(path)

  def reportError(path: String, message: String, e: Option[Throwable] = None): PlayException = {
    val origin = Option(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin)
    Configuration.configError(message, origin, e)
  }

}

object Configuration {

  private[yalp] def load(
                        classLoader: ClassLoader,
                        properties: Properties,
                        directSettings: Map[String, AnyRef],
                        allowMissingApplicationConf: Boolean
                        ): Configuration = {
    try {
      val systemPropertyConfig =
        if (properties eq System.getProperties) ConfigImpl.systemPropertiesAsConfig()
        else ConfigFactory.parseProperties(properties)

      val directConfig = ConfigFactory.parseMap(directSettings.asJava)

      val applicationConfig: Config = {
        def setting(key: String): Option[AnyRef] =
          directSettings.get(key) orElse Option(properties.getProperty(key))

        val configRsrc = setting("config.resource").map(resource => ConfigFactory.parseResources(classLoader, resource.toString))
        def configFile = setting("config.file").map(fileName => ConfigFactory.parseFileAnySyntax(new File(fileName.toString)))
        def defaults = {
          val parseOptions = ConfigParseOptions.defaults()
            .setClassLoader(classLoader)
            .setAllowMissing(allowMissingApplicationConf)

          ConfigFactory.defaultApplication(parseOptions)
        }

        configRsrc orElse configFile getOrElse defaults
      }

      val combinedConfig: Config = Seq(
        systemPropertyConfig, directConfig, applicationConfig
      ).reduceLeft(_ withFallback _)

      val resolvedConfig = combinedConfig.resolve

      Configuration(resolvedConfig)
    }
    catch { case e: ConfigException => throw configError(e.getMessage, Option(e.origin), Some(e))}
  }


  def load(environment: Environment, devSettings: Map[String, AnyRef]): Configuration = {
    load(environment.classLoader, System.getProperties, devSettings, allowMissingApplicationConf = environment.mode == Mode.Test)
  }

  private[api] def configError(message: String, origin: Option[ConfigOrigin] = None, e: Option[Throwable] = None) = {

    new api.PlayException.ExceptionSource("Configuration error", message, e.orNull) {

      private val originLine: Int = origin.fold(0)(_.lineNumber)
      private val originSourceName = origin.map(_.filename).orNull
      private val orignInput = for {
        o <- origin
        url <- Option(o.url)
      } yield PlayIO.readUrlAsString(url)


      override def line: Int = originLine
      override def input: String = orignInput.orNull
      override def sourceName: String = originSourceName
      override def position: Int = 0
      override def toString: String = "Configuration error: " + getMessage
    }
  }
}

trait ConfigLoader[A] {

  def load(config: Config, path: String = ""): A

}

object ConfigLoader {

  def apply[A](f: Config => String => A): ConfigLoader[A] =
    (config: Config, path: String) => f(config)(path)

  implicit val booleanLoader: ConfigLoader[Boolean] = ConfigLoader(_.getBoolean)
  implicit val stringLoader: ConfigLoader[String] = ConfigLoader(_.getString)


  implicit def optionLoader[A](implicit valueLoader: ConfigLoader[A]): ConfigLoader[Option[A]] = new ConfigLoader[Option[A]] {
    def load(config: Config, path: String): Option[A] = {
      if (config.getIsNull(path)) None else {
        val value = valueLoader.load(config, path)
        Some(value)
      }
    }
  }

}