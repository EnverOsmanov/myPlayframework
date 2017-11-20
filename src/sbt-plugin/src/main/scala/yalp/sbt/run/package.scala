package yalp.sbt

import sbt.Logger
import play.dev.filewatch.LoggerProxy

package object run {
  import scala.language.implicitConversions

  implicit def toLoggerProxy(in: Logger): LoggerProxy = new LoggerProxy {

    override def verbose(message: => String): Unit = in.verbose(message)

    override def debug(message: => String): Unit = in.debug(message)

    override def info(message: => String): Unit = in.info(message)

    override def warn(message: => String): Unit = in.warn(message)

    override def error(message: => String): Unit = in.error(message)

    override def trace(t: => Throwable): Unit = in.trace(t)

    override def success(message: => String): Unit = in.success(message)
  }
}
