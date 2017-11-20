package yalp.api

import scala.concurrent.Future

trait Application {

  def mode: Mode = environment.mode

  def environment: Environment


  def configuration: Configuration

  lazy val globalApplicationEnabled: Boolean = configuration
    .getOptional[Boolean](Play.GlobalAppConfigKey)
    .getOrElse(true)


  def classloader: ClassLoader

  def stop(): Future[_]

}
