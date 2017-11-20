package yalp.sbt.run

import sbt.{File, Incomplete, Result, ScopedKey, Task, file}
import sbt.Def.Classpath
import sbt.Keys.{Streams, taskDefinitionKey}
import sbt.internal.Output
import sbt.internal.inc.Analysis
import yalp.api.PlayException
import yalp.runsupport.Reloader.{CompileFailure, CompileResult, CompileSuccess, Source}
import yalp.sbt.PlayExceptions.{CompilationException, UnexpectedException}

object PlayReload {

  def originalSource(file: File): Option[File] = None

  def compileFailure(streams: Option[Streams])(incomplete: Incomplete): CompileResult =
    CompileFailure(taskFailureHandler(incomplete, streams))

  def taskFailureHandler(incomplete: Incomplete, streams: Option[Streams]): PlayException = {
    Incomplete.allExceptions(incomplete).headOption.map {
      case e: PlayException => e
      case e: xsbti.CompileFailed =>
        getProblems(incomplete, streams)
          .find(_.severity == xsbti.Severity.Error)
          .map(CompilationException)
          .getOrElse(UnexpectedException(Some("The compilation failed without reporting any problem!"), Some(e)))
      case e: Exception => UnexpectedException(unexpected = Some(e))
    }.getOrElse {
      UnexpectedException(Some("The compilation task failed without any exception!"))
    }
  }

  def getScopedKey(incomplete: Incomplete): Option[ScopedKey[_]] = incomplete.node flatMap {
    case key: ScopedKey[_] => Option(key)
    case task: Task[_] => task.info.attributes get taskDefinitionKey
  }

  def getProblems(incomplete: Incomplete, streams: Option[Streams]): Seq[xsbti.Problem] = {
    allProblems(incomplete) ++ {
      Incomplete.linearize(incomplete).flatMap(getScopedKey).flatMap { scopedKey =>
        val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
        val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
        val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

        streams.map { streamsManager =>
          var first: (Option[(String, String, String)], Option[Int]) = (None, None)
          var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
          Output.lastLines(scopedKey, streamsManager, None).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
            case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
            case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
              parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
            }
            case JavacErrorPosition(pos) =>
              parsed = parsed._1 -> Some(pos.size)
              if (first == ((None, None))) {
                first = parsed
              }
          }
          first
        }.collect {
          case (Some(error), maybePosition) => new xsbti.Problem {
            def message = error._3
            def category = ""
            def position = new xsbti.Position {
              def line = java.util.Optional.ofNullable(error._2.toInt)
              def lineContent = ""
              def offset = java.util.Optional.empty[java.lang.Integer]
              def pointer = maybePosition.map(pos => java.util.Optional.ofNullable((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(java.util.Optional.empty[java.lang.Integer])
              def pointerSpace = java.util.Optional.empty[String]
              def sourceFile = java.util.Optional.ofNullable(file(error._1))
              def sourcePath = java.util.Optional.ofNullable(error._1)
            }
            def severity = xsbti.Severity.Error
          }
        }

      }
    }
  }

  def allProblems(inc: Incomplete): Seq[xsbti.Problem] = {
    allProblems(inc :: Nil)
  }

  def allProblems(incs: Seq[Incomplete]): Seq[xsbti.Problem] = {
    problems(Incomplete.allExceptions(incs).toSeq)
  }

  def problems(es: Seq[Throwable]): Seq[xsbti.Problem] = {
    es flatMap {
      case cf: xsbti.CompileFailed => cf.problems
      case _ => Nil
    }
  }

  def sourceMap(analysis: Analysis): Map[String, Source] = {
    analysis
      .relations
      .classes
      .reverseMap
      .mapValues{ files =>
        val file = files.head
        Source(file, originalSource(file))
      }
  }

  def compile(reloadCompile: () => Result[Analysis],
              classpath: () => Result[Classpath],
              streams: () => Option[Streams]): CompileResult = {
    val compileResult: Either[Incomplete, CompileResult] = for {
      analysis <- reloadCompile().toEither
      classpath <- classpath().toEither
    } yield CompileSuccess(sourceMap(analysis), classpath.files)

    compileResult.left.map(compileFailure(streams())).merge
  }

}
