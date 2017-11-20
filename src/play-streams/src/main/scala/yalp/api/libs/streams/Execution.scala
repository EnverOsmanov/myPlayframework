package yalp.api.libs.streams

import java.util

import scala.annotation.tailrec
import scala.concurrent.ExecutionContextExecutor

private[yalp] object Execution {

  object StreamTrampoline extends ExecutionContextExecutor {

    private val local = new ThreadLocal[AnyRef]

    private object Empty

    override def execute(command: Runnable): Unit = {
      local.get match {
        case null =>
          try {
            local.set(Empty)
            command.run()
            executeScheduled()
          }
          finally local.set(null)

        case Empty => local.set(command)

        case next: Runnable =>
          val runnables = new util.ArrayDeque[Runnable](4)
          runnables.addLast(next)
          runnables.addLast(command)
          local.set(runnables)

        case arrayDeque: util.ArrayDeque[_] =>
          val runnables = arrayDeque.asInstanceOf[util.ArrayDeque[Runnable]]
          runnables.addLast(command)

        case illegal =>
          throw new IllegalStateException(s"Unsupported trampoline ThreadLocal value: $illegal")
      }
    }

    override def reportFailure(cause: Throwable): Unit =
      cause.printStackTrace()

    @tailrec
    private def executeScheduled(): Unit = {
      local.get match {
        case Empty => ()

        case next: Runnable =>
          local.set(Empty)
          next.run()
          executeScheduled()

        case arrayDeque: util.ArrayDeque[_] =>
          val runnables = arrayDeque.asInstanceOf[util.ArrayDeque[Runnable]]

          while (!runnables.isEmpty) {
            val runnable = runnables.removeFirst()
            runnable.run()
          }

        case illegal =>
          throw new IllegalStateException(s"Unsupported trampoline ThreadLocal value: $illegal")

      }
    }

  }

}
