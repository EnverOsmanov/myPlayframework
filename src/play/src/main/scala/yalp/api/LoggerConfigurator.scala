package yalp.api

import java.io.File
import java.util.Properties

trait LoggerConfigurator {

  def init(rootPath: File, mode: Mode): Unit

}

object LoggerConfigurator {

  def apply(classLoader: ClassLoader): Option[LoggerConfigurator] = {
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
    Option(loader.getResourceAsStream("logger-configurator.properties")).flatMap { in =>

      try {
        val props = new Properties()
        props.load(in)
        Option(props.getProperty("yalp.logger.configuration"))
      }
      catch {
        case ex: Exception =>
          ex.printStackTrace()
          None
      }
    }
  }

}
