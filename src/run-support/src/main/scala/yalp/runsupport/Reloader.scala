package yalp.runsupport

import java.io.{Closeable, File}
import java.net.URL
import java.security.{AccessController, PrivilegedAction}
import java.util.concurrent.atomic.AtomicInteger

import play.dev.filewatch.FileWatchService
import yalp.api.PlayException
import yalp.core.server.ReloadableServer
import yalp.core.{Build, BuildLink}
import yalp.runsupport.classloader.{ApplicationClassLoaderProvider, DelegatingClassLoader}
import better.files.FileOps
import yalp.runsupport.Reloader._

object Reloader {

  sealed trait CompileResult
  case class CompileSuccess(sources: Map[String, Source], classpath: Seq[File]) extends CompileResult
  case class CompileFailure(exception: PlayException) extends CompileResult

  trait GeneratedSourceMapping {
    def getOriginalLine(generatedSource: File, line: Int): Int
  }

  case class Source(file: File, original: Option[File])

  def parsePort(portStr: String): Int = {
    try Integer.parseInt(portStr)
    catch { case _: NumberFormatException => sys.error(s"Invalid port argument: $portStr")}
  }

  def filterArgs(
                  args: Seq[String],
                  defaultHttpPort: Int,
                  defaultHttpAddress: String,
                  devSettings: Seq[(String, String)]
                ): (Seq[(String, String)], Option[Int], Option[Int], String) = {
    val (propertyArgs, otherArgs) = args.partition(_.startsWith("-D"))

    val properties = propertyArgs.map {
      _.drop(2).span(_ != '=') match {
        case (key, v) => key -> v.tail
      }
    }

    val props = properties.toMap

    def prop(key: String) = props.get(key) orElse sys.props.get(key)

    def parsePortValue(portValue: Option[String], defaultValue: Option[Int]) = {
      portValue match {
        case None => defaultValue
        case Some("disabled") => None
        case Some(s) => Some(parsePort(s))
      }
    }

    val httpPortString: Option[String] = otherArgs.headOption
    val httpPort: Option[Int] = parsePortValue(httpPortString, Option(defaultHttpPort))

    val httpsPortString: Option[String] = prop("https.port")
    val httpsPort: Option[Int] = parsePortValue(httpsPortString, Option(defaultHttpPort))

    val httpAddress = prop("http.address") getOrElse defaultHttpAddress

    (properties, httpPort, httpsPort, httpAddress)
  }

  def urls(cp: Seq[File]): Array[URL] = cp.map(_.toURI.toURL)(collection.breakOut)

  trait DevServer extends Closeable {
    val buildLink: BuildLink
    def addChangeListener(f: () => Unit): Unit
    def reload(): Unit
    def url(): String

  }

  def startDevMode(runHooks: Seq[RunHook],

                   commonClassLoader: ClassLoader,

                   reloadCompile: () => CompileResult,
                   assetsClassLoader: ClassLoader => ClassLoader,
                   projectPath: File,
                   monitoredFiles: Seq[File],
                   reloadLock: AnyRef,

                   fileWatchService: FileWatchService,
                   generatedSourceHandlers: Map[String, GeneratedSourceMapping],

                   dependencyClasspath: Seq[File],
                   defaultHttpPort: Int, defaultHttpAddress: String,// projectPath: File,
                   devSettings: Seq[(String, String)], args: Seq[String],
                   mainClassName: String): DevServer = {

    val (properties, httpPort, httpsPort, httpAddress) = filterArgs(args, defaultHttpPort, defaultHttpAddress, devSettings)

    val buildLoader = this.getClass.getClassLoader

    lazy val delegatingLoader: ClassLoader = {
      val appCLProvier = new ApplicationClassLoaderProvider {
        override def get: ClassLoader = {
          println(s"PROVIDER ${reloader.getClassLoader}")
          reloader.getClassLoader.orNull
        }
      }

      new DelegatingClassLoader(commonClassLoader, Build.sharedClasses, buildLoader, appCLProvier)
    }

    lazy val applicationLoader = new NamedURLClassLoader("DependencyClassLoader", urls(dependencyClasspath), delegatingLoader)

    lazy val assetsLoader = assetsClassLoader(applicationLoader)

    lazy val reloader = new Reloader(
      reloadCompile, assetsLoader, projectPath, devSettings, monitoredFiles,
      fileWatchService, generatedSourceHandlers, reloadLock
    )

    try {
      runHooks.run(_.beforeStarted())

      val server = {
        val mainClass = applicationLoader.loadClass(mainClassName)

        if (httpPort.isDefined) {

          println(s"MAP RELOADER_OBJ: ${getClass.getClassLoader}")
          println(s"MAP RELOADER_INS: ${reloader.getClass.getClassLoader}")
          println(s"MAP RELOADER_SET: ${reloader.settings.getClass.getClassLoader}")
          val mainDev = mainClass.getMethod("mainDevHttpMode", classOf[BuildLink], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, httpPort.get: java.lang.Integer, httpAddress).asInstanceOf[ReloadableServer]
        }
        else {
          val mainDev = mainClass.getMethod("mainDevHttpsMode", classOf[BuildLink], classOf[Int], classOf[String])
          mainDev.invoke(null, reloader, httpsPort.get: java.lang.Integer, httpAddress).asInstanceOf[ReloadableServer]
        }
      }

      runHooks.run(_.afterStarted(server.mainAddress))

      new DevServer {
        override val buildLink: BuildLink = reloader
        override def addChangeListener(f: () => Unit): Unit = reloader.addChangeListener(f)
        override def reload(): Unit = server.reload()
        override def close(): Unit = {
          server.stop()
          reloader.close()

          runHooks.run(_.afterStopped())

          properties.foreach {
            case (key, _) => System.clearProperty(key)
          }
        }

        override def url(): String = server.mainAddress.getHostName
      }
    }
    catch {
      case e: Throwable =>
        // Let hooks clean up
        runHooks.foreach { hook =>
          try {
            hook.onError()
          } catch {
            case _: Throwable => // Swallow any exceptions so that all `onError`s get called.
          }
        }

        // Convert play-server exceptions to our to our ServerStartException
        def getRootCause(t: Throwable): Throwable = if (t.getCause == null) t else getRootCause(t.getCause)

        if (getRootCause(e).getClass.getName == "play.core.server.ServerListenException") {
          throw new ServerStartException(e)
        }
        throw e
    }

  }

  private def withReloaderContextClassLoader[T](f: => T): T = {
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader

    AccessController.doPrivileged(new PrivilegedAction[T] {
      override def run(): T = {
        try {
          thread.setContextClassLoader(classOf[Reloader].getClassLoader)
          f
        }
        finally thread.setContextClassLoader(oldLoader)
      }
    })
  }


}

class Reloader(reloadCompile: () => CompileResult,
               baseLoader: ClassLoader,
               val projectPath: File,
               devSettings: Seq[(String, String)],
               monitoredFiles: Seq[File],
               fileWatchService: FileWatchService,
               generatedSourceHandlers: Map[String, GeneratedSourceMapping],
               reloadLock: AnyRef
              ) extends BuildLink {

  @volatile private var currentSourceMap: Option[Map[String, Source]] = Option.empty

  @volatile private var currentApplicationClassLoader: Option[ClassLoader] = None

  @volatile private var changed = false
  @volatile private var forceReloadNextTime = false
  @volatile private var lastModified: Long = 0L


  private val classLoaderVersion = new AtomicInteger(0)
  private val watcher = fileWatchService.watch(monitoredFiles, () => {changed = true})

  private val listeners = new java.util.concurrent.CopyOnWriteArrayList[() => Unit]()

  def getClassLoader: Option[ClassLoader] = currentApplicationClassLoader

  def addChangeListener(f: () => Unit): Unit = listeners.add(f)


  override def settings: Map[String, String] = devSettings.toMap

  override def reload: AnyRef = {
    reloadLock.synchronized(
      if (changed || forceReloadNextTime || currentSourceMap.isEmpty || currentApplicationClassLoader.isEmpty) {
        val shouldReload = forceReloadNextTime

        changed = false
        forceReloadNextTime = false

        Reloader.withReloaderContextClassLoader {
          reloadCompile() match {
            case CompileFailure(exception) =>
              forceReloadNextTime = true
              exception

            case CompileSuccess(sourceMap, classpath) =>
              currentSourceMap = Some(sourceMap)

              val classpathFiles = classpath.iterator.filter(_.exists()).flatMap(_.toScala.listRecursively).map(_.toJava)

              val newLastModified = (0L /: classpathFiles) ( (acc, file) => math.max(acc, file.lastModified))

              val triggered = newLastModified > lastModified
              lastModified = newLastModified

              if (triggered || shouldReload || currentApplicationClassLoader.isEmpty) {
                val version = classLoaderVersion.incrementAndGet
                val name = s"ReloadableClassLoader(v$version)"
                val urls = Reloader.urls(classpath)
                val loader = new DelegatedResourcesClassLoader(name, urls, baseLoader)
                currentApplicationClassLoader = Some(loader)
                loader
              }
              else null
          }
        }
      }
      else null
    )
  }

  override def findSource(className: String, line: Integer): Array[AnyRef] = {
    val topType = className.split("$").head

    val maybeSources: Option[Array[AnyRef]] = for {
      sources <- currentSourceMap
      source <- sources.get(topType)
    } yield source.original match {
      case Some(origFile) if line != null =>
        generatedSourceHandlers.get(origFile.getName.split('.').drop(1).mkString(".")) match {
          case Some(handler) =>
            val originalLine: Integer = handler.getOriginalLine(source.file, line)
            Array[AnyRef](origFile, originalLine)

          case _ => Array[AnyRef](origFile, line)
        }

      case Some(origFile) => Array(origFile, null)
      case None => Array[AnyRef](source.file, line)
    }

    maybeSources.orNull
  }

  def close() = {
    currentApplicationClassLoader = None
    //      currentSourceMap = None
    watcher.stop()
    //      quietTimeTimer.cansel()
  }

}
