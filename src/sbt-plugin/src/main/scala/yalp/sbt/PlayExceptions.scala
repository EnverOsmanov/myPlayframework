package yalp.sbt

import sbt.io.IO

import yalp.api.PlayException
import scala.language.implicitConversions


object PlayExceptions {

  private def filterAnnoyingErrorMessages(message: String): String = {
    val overloaded = """(?s)overloaded method value (.*) with alternatives:(.*)cannot be applied to(.*)""".r
    message match {
      case overloaded(method, _, signature) => "Overloaded method value [" + method + "] cannot be applied to " + signature
      case msg => msg
    }
  }

  case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None) extends PlayException(
    "Unexpected exception",
    message.getOrElse {
      unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
    },
    unexpected.orNull
  )

  case class CompilationException(problem: xsbti.Problem) extends PlayException.ExceptionSource(
    "Compilation error", filterAnnoyingErrorMessages(problem.message), null) {
    override def line: Int = problem.position.line.orElseGet(null)
    override def position: Int = problem.position.pointer.orElseGet(null)
    override def input: String = problem.position.sourceFile.map[String](IO.read(_)).orElseGet(null)
    override def sourceName: String = problem.position.sourceFile.map[String](_.getAbsolutePath).orElseGet(null)
  }
}
