package yalp.core

import yalp.api.{PlayException, UsefulException}
import yalp.core.server.ReloadableServer

object Build {

  val sharedClasses: Array[String] = Array(
    classOf[BuildLink].getName,
    classOf[ReloadableServer].getName,
    classOf[UsefulException].getName,
    classOf[PlayException].getName,
    classOf[PlayException.ExceptionSource].getName,
  )

}
