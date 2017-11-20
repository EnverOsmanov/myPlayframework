package yalp.core.server

import java.io.File

import yalp.api.mvc.{RequestHeader, Result}
import yalp.api._
import yalp.api.inject.DefaultApplicationLifecycle
import yalp.core.system.WebCommands
import yalp.core.{ApplicationProvider, BuildLink, SourceMapper}
import yalp.utils.{Colors, Threads}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

object DevServerStart {

  def mainDevHttpMode(
                     buildLink: BuildLink, httpPort: Int, httpAddress: String
                     ): ReloadableServer =
    mainDev(buildLink, Some(httpPort), Option(System.getProperty("https.port")).map(_.toInt), httpAddress)

  def mainDev(buildLink: BuildLink,
              httpPort: Option[Int],
              httpsPort: Option[Int],
              httpAddress: String): ReloadableServer = {
    val classLoader = getClass.getClassLoader

    println(s"MAP BUILDLINK: ${classLoader}")
    Threads.withContextClassLoader(classLoader) {
      try {
        val process = new RealServerProcess(Nil)
        val path = buildLink.projectPath

        println(s"MAP BUILDLINK: ${buildLink.settings.getClass.getClassLoader}")

        val dirAndDevSettings = ServerConfig.rootDirConfig(path) ++ buildLink.settings

        // Use plain Java call here in case of scala classloader mess
        {
          if (System.getProperty("yalp.debug.classpath") == "true") {
            System.out.println("\n---- Current ClassLoader ----\n")
            System.out.println(this.getClass.getClassLoader)
            System.out.println("\n---- The where is Scala? test ----\n")
            System.out.println(this.getClass.getClassLoader.getResource("scala/Predef$.class"))
          }
        }

        try new File(path, "logs/application.log").delete()
        catch { case NonFatal(_) => }

        LoggerConfigurator(this.getClass.getClassLoader) match {
          case Some(loggerConfigurator) =>
            loggerConfigurator.init(path, Mode.Dev)

          case None =>
            println("No play.logger.configurator found: logging must be configured entirely by the application.")
        }

        println(Colors.magenta("--- (Running the application, auto-reloading is enabled) ---"))
        println()

        val appProvider = new ApplicationProvider {

          val sl = new java.util.concurrent.locks.StampedLock

          var lastState: Try[Application] = Failure(new PlayException("Not initialized", "?"))
          var lastLifecycle: Option[DefaultApplicationLifecycle] = None
          var currentWebCommands: Option[WebCommands] = None

          override def current: Option[Application] = lastState.toOption

          override def get: Try[Application] = synchronized(
            buildLink.reload match {
              case cl: ClassLoader => reload(cl)
              case null => lastState
              case NonFatal(t) => Failure(t)
              case t: Throwable => throw t
            }
          )

          override def handleWebCommand(request: RequestHeader): Option[Result] = {
            None
          }

          private def reload(projectClassloader: ClassLoader): Try[Application] = {
            try {
              if (lastState.isSuccess) {
                  println()
                  println(Colors.magenta("--- (RELOAD) ---"))
                  println()
              }

              val reloadable = this

              lastState.foreach(Play.stop)

              lastLifecycle.foreach(cycle => Await.result(cycle.stop(), 10.minutes))

              val environment = Environment(path, projectClassloader, Mode.Dev)
              val sourceMapper = new SourceMapper {
                override def sourceOf(className: String, line: Option[Int]): Option[(File, Option[Int])] = {
                  val lineInteger = line.map(_.asInstanceOf[java.lang.Integer]).orNull

                  Option(buildLink.findSource(className, lineInteger)).flatMap{
                    case Array(file: File, null) => Some(file -> None)
                    case Array(file: File, line: Integer) => Some(file -> Some(line))
                    case _ => None
                  }
                }
              }

              val lifecycle = new DefaultApplicationLifecycle()
              lastLifecycle = Some(lifecycle)

              val newApplication = Threads.withContextClassLoader(projectClassloader) {
                val context = ApplicationLoader.createContext(environment, Some(sourceMapper), dirAndDevSettings, lifecycle)
                val loader = ApplicationLoader(context)
                loader.load(context)
              }

              Play.start(newApplication)
              lastState = Success(newApplication)
              lastState
            }
            catch {
              case e: PlayException =>
                lastState = Failure(e)
                lastState

              case NonFatal(e) =>
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                lastState


              case e: LinkageError =>
                lastState = Failure(UnexpectedException(unexpected = Some(e)))
                lastState
            }
          }

        }

        val serverConfig = ServerConfig(
          rootDir = path,
          port = httpPort,
          sslPort = httpsPort,
          address = httpAddress,
          mode = Mode.Dev,
          properties = process.properties,
          configuration = Configuration.load(classLoader, System.getProperties, dirAndDevSettings, allowMissingApplicationConf = true)
        )

        val devModeAkkaConfig = serverConfig.configuration.underlying

        val serverContext = ServerProvider.Context(serverConfig, appProvider, () => Future.successful(()))

        val serverProvider = ServerProvider.fromConfiguration(classLoader, serverConfig.configuration)
        serverProvider.createServer(serverContext)
      }
      catch { case e: ExceptionInInitializerError =>

        println(s"MAP BUILDLINK: ${buildLink.settings.getClass.getClassLoader}")
        throw e.getCause }
    }

  }

}
