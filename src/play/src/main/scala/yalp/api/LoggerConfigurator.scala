package yalp.api

import java.io.{BufferedReader, File, InputStreamReader}
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.Properties

import com.typesafe.config.ConfigValueType
import org.slf4j.ILoggerFactory

trait LoggerConfigurator {

  def init(rootPath: File, mode: Mode): Unit

  def configure(env: Environment): Unit

  def configure(properties: Map[String, String], config: Option[URL]): Unit

  def configure(env: Environment, configuration: Configuration, optionalProperties: Map[String, String]): Unit

  def loggerFactory: ILoggerFactory

}

object LoggerConfigurator {


  def generateProperties(env: Environment, config: Configuration, optionalProperties: Map[String, String]): Map[String, String] = {
    import scala.collection.JavaConverters._
    val mutableMap: Map[String, String] = {
      if (config.getOptional[Boolean]("play.logger.includeConfigProperties").contains(true)) {
        val entrySet = config.underlying.entrySet().asScala

        (for (entry <- entrySet) yield {
          val value = entry.getValue

          value.valueType() match {
            case ConfigValueType.STRING => entry.getKey -> value.unwrapped().asInstanceOf[String]

            case _ => entry.getKey -> value.render()
          }
        }) (collection.breakOut)
      }
      else Map.empty
    }

    val init = Map("application.home" -> env.rootPath.getAbsolutePath)

    init ++ mutableMap ++ optionalProperties
  }

  def apply(classLoader: ClassLoader): Option[LoggerConfigurator] = {
    println(s"LOGGER cL_1: $classLoader")
    findFromResources(classLoader).flatMap ( className =>
      apply(className, classLoader)
    )
  }

  def apply(loggerConfiguratorClassName: String, classLoader: ClassLoader): Option[LoggerConfigurator] = {
    try {
      val loggerConfiguratorClass: Class[_] = classLoader.loadClass(loggerConfiguratorClassName)
      Some(loggerConfiguratorClass.newInstance().asInstanceOf[LoggerConfigurator])
    }
    catch { case ex: Exception =>
      val msg =
        s"""
           |Play cannot load "$loggerConfiguratorClassName". Please make sure you have logback (or another module
           |that implements play.api.LoggerConfigurator) in your classpath.
             """.stripMargin
      System.err.println(msg)
      ex.printStackTrace()
      None
    }
  }

  private def findFromResources(loader: ClassLoader): Option[String] = {
    println(s"LOGGER loader: $loader")

    val is = Paths.get("./src/main")
    if (is == null) println(s"Logger IS is null, $is")
    println(is)
    //println(is.getPath)
    val path = is.toFile.listFiles().toList.toString()

      println(s"Logger RSRS: ${path}line")


    Option(loader.getResourceAsStream("logger-configurator.properties")).flatMap { in =>
      println(s"LOGGER stream: $in")
      try {
        val props = new Properties()
        props.load(in)
        println(s"LOGGER PROPS: $props")
        Option(props.getProperty("yalp.logger.configurator"))
      }
      catch {
        case ex: Exception =>
          ex.printStackTrace()
          None
      }
      finally in.close()
    }
  }

}
