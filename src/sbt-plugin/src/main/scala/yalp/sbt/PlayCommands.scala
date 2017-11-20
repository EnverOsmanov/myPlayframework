package yalp.sbt

import java.net.{URL, URLClassLoader}
import java.nio.file.Path

import sbt.Keys._
import sbt.{Compile, Def, File, ScopeFilter, Task}

object PlayCommands {


  val playMonitoredFilesTask: Def.Initialize[Task[Seq[File]]] = Def.taskDyn{
    val projectRef = thisProjectRef.value

    def filter = ScopeFilter(
      sbt.inDependencies(projectRef),
      sbt.inConfigurations(Compile)
    )

    Def.task {

      val allDirectories: Seq[File] =
        (unmanagedSourceDirectories ?? Nil).all(filter).value.flatten ++
        (unmanagedResourceDirectories ?? Nil).all(filter).value.flatten

      val existingDirectories = allDirectories.filter(_.exists)

      val distinctDirectories = existingDirectories
        .map(_.getCanonicalFile.toPath)
        .sorted.foldLeft(List.empty[Path]) { (result, next) =>
        result.headOption match {
          case Some(previous) if next.startsWith(previous) => result
          case _ => next :: result
        }
      }

      distinctDirectories.map(_.toFile)

    }
  }

  private[this] var commonClassLoader: ClassLoader = _

  val playCommonClassloaderTask = Def.task {
    val classpath = (dependencyClasspath in Compile).value

    val log = streams.value.log

    lazy val commonJars: PartialFunction[File, URL] = {
      case jar if jar.getName.startsWith("h2-") || jar.getName == "h2.jar" =>
        jar.toURI.toURL
    }

    if (commonClassLoader == null) {
      val parent = ClassLoader.getSystemClassLoader.getParent
      log.warn(s"Using parent loader for play common classloader: $parent")

      commonClassLoader = new URLClassLoader(classpath.map(_.data).collect(commonJars).toArray, parent) {
        override def toString = s"Common ClassLoader: ${getURLs.map(_.toString).mkString(",")}"
      }
    }

    commonClassLoader
  }

/*  val playCompileEverythingTask = Def.taskDyn {
    val compileTask = Def.taskDyn()
  }*/

}
