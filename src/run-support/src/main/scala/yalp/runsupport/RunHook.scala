package yalp.runsupport

import java.net.InetSocketAddress

trait RunHook {

  /**
    * Called before the play application is started,
    * but after all "before run" tasks have been completed.
    */
  def beforeStarted(): Unit = ()

  /**
    * Called after the play application has been started.
    * @param addr The address/socket that play is listening to
    */
  def afterStarted(addr: InetSocketAddress): Unit = ()

  /**
    * Called after the play process has been stopped.
    */
  def afterStopped(): Unit = ()

  /**
    * Called if there was any exception thrown during play run.
    * Useful to implement to clean up any open resources for this hook.
    */
  def onError(): Unit = ()

}

object RunHook {

  implicit class RunHooksRunner(val hooks: Seq[RunHook]) extends AnyVal {

    def run(f: RunHook => Unit, suppressFailure: Boolean = false): Unit = try {

    }
  }

}
