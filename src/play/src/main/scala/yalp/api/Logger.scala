package yalp.api

import org.slf4j.{LoggerFactory, Marker, Logger => Slf4jLogger}

trait LoggerLike {

  val logger: Slf4jLogger

  @inline def enabled: Boolean = true

  /**
    * `true` if the logger instance is enabled for the `WARN` level.
    */
  def isWarnEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isWarnEnabled()
    case Some(marker) =>
      logger.isWarnEnabled(marker)
  })

  /**
    * `true` if the logger instance is enabled for the `ERROR` level.
    */
  def isErrorEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isErrorEnabled()
    case Some(marker) =>
      logger.isErrorEnabled(marker)
  })

  def isInfoEnabled(implicit mc: MarkerContext): Boolean = enabled && (mc.marker match {
    case None =>
      logger.isInfoEnabled
    case Some(marker) =>
      logger.isInfoEnabled(marker)
  })

  def error(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isErrorEnabled) {
      mc.marker match {
        case None => logger.error(message, error)
        case Some(marker) => logger.error(marker, message, error)
      }
    }
  }

  /**
    * Logs a message with the `WARN` level.
    *
    * @param message the message to log
    * @param error the associated exception
    * @param mc the implicit marker context, if defined.
    */
  def warn(message: => String, error: => Throwable)(implicit mc: MarkerContext): Unit = {
    if (isWarnEnabled) {
      mc.marker match {
        case None => logger.warn(message, error)
        case Some(marker) => logger.warn(marker, message, error)
      }
    }
  }

  def info(message: => String)(implicit mc: MarkerContext): Unit = {
    if (isInfoEnabled) {
      mc.marker match {
        case None => logger.info(message)
        case Some(marker) => logger.info(marker, message)
      }
    }
  }

}

class Logger private(val logger: Slf4jLogger, isEnabled: => Boolean) extends LoggerLike {


  def this(logger: Slf4jLogger) =
    this(logger, true)



}

object Logger extends Logger(LoggerFactory.getLogger("application")) {

  def apply(clazz: Class[_]): Logger = new Logger(
    LoggerFactory.getLogger(clazz.getName.stripSuffix("$"))
  )

}

trait MarkerContext {

  def marker: Option[Marker]

}

object MarkerContext extends LowPriorityMarkerContextImplicits {
  def apply(marker: Marker): MarkerContext = {
    new DefaultMarkerContext(marker)
  }
}

trait LowPriorityMarkerContextImplicits {

  implicit val NoMarker: MarkerContext =
    MarkerContext(null)

}

class DefaultMarkerContext(someMarker: Marker) extends MarkerContext {
  def marker: Option[Marker] = Option(someMarker)
}
