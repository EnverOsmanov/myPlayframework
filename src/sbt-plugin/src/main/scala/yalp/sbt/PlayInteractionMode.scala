package yalp.sbt

import java.io.Closeable

import jline.console.ConsoleReader

trait PlayInteractionMode {
  def waitForCancel(): Unit

  /**
    * Enables and disables console echo (or does nothing if no console).
    * This ensures console echo is enabled on exception thrown in the
    * given code block.
    */
  def doWithoutEcho(f: => Unit): Unit
}

trait PlayNonBlockingInteractionMode extends PlayInteractionMode {

  def start(server: => Closeable): Unit
}

object PlayConsoleInteractionMode extends PlayInteractionMode {

  private def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = new ConsoleReader()

    try f(consoleReader)
    finally consoleReader.close()
  }

  private def waitForKey(): Unit = withConsoleReader {
    consoleReader =>

      def waitEOF(): Unit = {
        consoleReader.readCharacter() match {
          case 4 | 13 | -1 =>

          case 11 =>
            consoleReader.clearScreen()
            waitEOF()

          case 10 =>
            println()
            waitEOF()

          case _ => waitEOF()
        }
      }

      doWithoutEcho(waitEOF())
  }


  override def waitForCancel(): Unit = waitForKey()

  /**
    * Enables and disables console echo (or does nothing if no console).
    * This ensures console echo is enabled on exception thrown in the
    * given code block.
    */
  override def doWithoutEcho(f: => Unit): Unit = withConsoleReader {
    consoleReader =>

      val terminal = consoleReader.getTerminal

      terminal.setEchoEnabled(false)

      try f
      finally terminal.restore()
  }

  override def toString = "Console Interaction Mode"
}