package yalp.sbt.run

import play.dev.filewatch.{SourceModificationWatch, WatchState}
import sbt.Keys._
import sbt.internal.util.AttributeKey
import sbt.{Compile, Def, InputTask, Keys, Project, State, TaskKey, Watched}
import yalp.core.BuildLink
import yalp.runsupport.Reloader.GeneratedSourceMapping
import yalp.runsupport.{AssetsClassLoader, Reloader}
import yalp.sbt.PlayImport.PlayKeys._
import yalp.sbt.PlayInternalKeys._
import yalp.sbt.{Colors, PlayNonBlockingInteractionMode, PlayRunHook}

import scala.annotation.tailrec


object PlayRun extends PlayRunCompat {
  val playAllAssetsSetting = playAllAssets := Nil // Seq(playPrefixAndAssets.value)

/*  val playPrefixAndAssetsSetting = playPrefixAndAssets := {
    assetsPrefix.value -> (WebKeys.public in Assets).value
  }*/

  val playAssetsClassLoaderSetting = playAssetsClassLoader := {
    val playAllAssetsValue = playAllAssets.value

    parent => new AssetsClassLoader(parent, playAllAssetsValue)
  }


  val playDefaultRunTask = playRunTask(playRunHooks, playDependencyClasspath, playReloaderClasspath, playAssetsClassLoader)

  val generatedSourceHandlers = Map.empty[String, GeneratedSourceMapping]


  def playRunTask(runHooks: TaskKey[Seq[PlayRunHook]],
                  dependencyClasspath: TaskKey[Classpath],
                  reloaderClasspath: TaskKey[Classpath],
                  assetsClassLoader: TaskKey[ClassLoader => ClassLoader]
                 ): Def.Initialize[InputTask[Unit]] = Def.inputTask{
    println("THIS IS PLAY run task")
    println(s"pDepCPath: ${playDependencyClasspath.value}")
    println()
    println(s"externalDepCP ${(externalDependencyClasspath in sbt.Runtime).value}")
    println()

    val args = Def.spaceDelimited().parsed

    val state = Keys.state.value
    val scope = Keys.resolvedScoped.value.scope
    val interaction = playInteractionMode.value

    val reloadCompile = () => PlayReload.compile(
      () => Project.runTask(playReload in scope, state).map(_._2).get,
      () => Project.runTask(reloaderClasspath in scope, state).map(_._2).get,
      () => Project.runTask(streamsManager in scope, state).map(_._2).get.toEither.right.toOption
    )

    lazy val devModeServer = Reloader.startDevMode(
      runHooks.value,
      playCommonClassloader.value,
      reloadCompile,
      assetsClassLoader.value,
      baseDirectory.value,
      playMonitoredFiles.value,
      reloadLock = PlayRun,
      fileWatchService.value,
      generatedSourceHandlers,
      dependencyClasspath = dependencyClasspath.value.files,
      defaultHttpPort = playDefaultPort.value,
      defaultHttpAddress = playDefaultAddress.value,
      devSettings = devSettings.value,
      args,
      mainClassName = (mainClass in (Compile, Keys.run)).value.get
    )

    interaction match {
      case nonBlocking: PlayNonBlockingInteractionMode =>
        nonBlocking.start(devModeServer)

      case _ =>
        devModeServer

        println()
        println(Colors.green("(Server started, use Enter to stop and go back to the console...)"))
        println()

        val maybeContinuous = for {
          watched <- state.get(Watched.Configuration)
          watchState <- state.get(Watched.ContinuousState)
          if watchState.count == 1
        } yield watched

        maybeContinuous match {
          case Some(watched) =>
            interaction doWithoutEcho {
              twiddleRunMonitor(watched, state, devModeServer.buildLink)
            }

          case None =>
            interaction.waitForCancel()
        }

    }
  }

  @tailrec
  private def twiddleRunMonitor(watched: Watched, state: State, reloader: BuildLink, ws: Option[WatchState] = None): Unit = {
    val ContinuousState = AttributeKey[WatchState]("watch state", "Internal: tracks state for continuous execution.")
    def isEOF(c: Int): Boolean = c == 4

    @tailrec def shouldTerminate: Boolean = (System.in.available() > 0) && (isEOF(System.in.read()) || shouldTerminate)


    val sourcesFinder: SourceModificationWatch.PathFinder = getSourcesFinder(watched, state)
    val watchState = ws.getOrElse(state get ContinuousState getOrElse WatchState.empty)

    val (triggered, newWatchState, newState) =
      try {
        val (triggered: Boolean, newWatchState: WatchState) = SourceModificationWatch.watch(sourcesFinder, getPollInterval(watched), watchState)(shouldTerminate)
        (triggered, newWatchState, state)
      } catch {
        case e: Exception =>
          val log = state.log
          log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
          log.trace(e)
          (false, watchState, state.fail)
      }

    if (triggered) {
      //Then launch compile
      Project.synchronized {
        val start = System.currentTimeMillis
        Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
          val duration = System.currentTimeMillis - start
          val formatted = duration match {
            case ms if ms < 1000 => ms + "ms"
            case seconds => (seconds / 1000) + "s"
          }
          println("[" + Colors.green("success") + "] Compiled in " + formatted)
        }
      }

      // Avoid launching too much compilation
      sleepForPoolDelay

      // Call back myself
      twiddleRunMonitor(watched, newState, reloader, Some(newWatchState))
    } else {
      ()
    }

  }

}
