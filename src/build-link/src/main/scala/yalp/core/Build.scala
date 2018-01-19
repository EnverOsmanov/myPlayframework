package yalp.core

import yalp.api.{PlayException, UsefulException}
import yalp.core.server.ReloadableServer

import scala.collection.generic.CanBuildFrom
import scala.collection.{GenMap, GenTraversableOnce, LinearSeqOptimized}
import scala.reflect.ClassTag

object Build {

  val sharedClasses: Array[String] = Array(
    classOf[BuildLink].getName,
    classOf[ReloadableServer].getName,
    classOf[UsefulException].getName,
    classOf[PlayException].getName,
    classOf[PlayException.ExceptionSource].getName
  )

}
