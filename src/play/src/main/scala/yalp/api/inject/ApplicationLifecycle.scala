package yalp.api.inject

import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject

import yalp.api.Logger

import scala.annotation.tailrec
import scala.concurrent.Future

trait ApplicationLifecycle {

  def stop(): Future[_]

}


class DefaultApplicationLifecycle @Inject()() extends ApplicationLifecycle {

  private val hooks = new ConcurrentLinkedDeque[() => Future[_]]()

  override def stop(): Future[_] = {
    import yalp.core.Execution.Implicits.trampoline

    @tailrec
    def clearHooks(previous: Future[Any] = Future.successful(())): Future[Any] = {
      val hook = hooks.poll()

      if (hook != null)  clearHooks(previous.flatMap(_ =>
        hook().recover {
          case e => Logger.error("Error executing stop hook", e)
        }
      ))
      else previous
    }

    clearHooks()
  }

}
