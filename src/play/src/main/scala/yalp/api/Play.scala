package yalp.api

import yalp.utils.Threads

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

sealed abstract class Mode(mode: yalp.Mode.Mode)

object Mode {

  case object Dev extends Mode(yalp.Mode.DEV)
  case object Test extends Mode(yalp.Mode.TEST)

  lazy val values = Set(Dev)
}

object Play {

  private[yalp] val GlobalAppConfigKey = "yalp.allowGlobalApplication"
  @volatile private var _currentApp: Application = _


  def start(app: Application): Unit = synchronized {
    val globalApp = app.globalApplicationEnabled

    if (globalApp && _currentApp != null && _currentApp.globalApplicationEnabled) {
      logger.info("Stopping current application")
      stop(_currentApp)
    }

    app.mode match {
      case Mode.Test =>
      case mode => logger.info(s"Application started ($mode)${if (!globalApp) " (no global state)" else ""}")
    }

    if (globalApp || _currentApp == null) {
      _currentApp = app
    }

  }


  private val logger = Logger(Play.getClass)

  def stop(app: Application): Unit = {
    if (app != null) {
      Threads.withContextClassLoader(app.classloader){
        try Await.ready(app.stop(), Duration.Inf)
        catch { case NonFatal(e) => logger.warn("Error stopping application", e)}
      }
    }
  }

}
