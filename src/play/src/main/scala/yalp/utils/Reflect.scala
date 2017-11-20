package yalp.utils

import yalp.api.{Configuration, Environment, PlayException}

import scala.reflect.ClassTag

object Reflect {

  def configuredClass[ScalaTrait, Default <: ScalaTrait]
  (environment: Environment, config: Configuration, key: String, defaultClassName: String)
  (implicit scalaTrait: SubClassOf[ScalaTrait], default: ClassTag[Default]): Option[Class[_ <: ScalaTrait]] = {

    def loadClass(className: String, notFoundFatal: Boolean): Option[Class[_]] = {
      try Some(environment.classLoader.loadClass(className))
      catch {
        case _: ClassNotFoundException if !notFoundFatal => None
        case e: VirtualMachineError => throw e
        case e: ThreadDeath => throw e
        case e: Throwable =>
          throw new PlayException(s"Cannot load $key", s"key [$className] was not loaded.", e)
      }
    }


    val maybeClass = config.get[Option[String]](key) match {
      case Some("provided") => None
      case None =>
        loadClass(defaultClassName, notFoundFatal = false)
          .orElse(Some(default.runtimeClass))

      case Some(className) =>
        loadClass(className, notFoundFatal = true)
    }

    maybeClass.map {
      case scalaTrait(scalaClass) => scalaClass

      case unknown =>
        throw new PlayException(s"Cannot load $key", s"$key [${unknown.getClass}}] does not implement ${scalaTrait.runtimeClass}.")

    }
  }

  class SubClassOf[T](val runtimeClass: Class[T]) {

    def unapply(clazz: Class[_]): Option[Class[_ <: T]] = {
      if (runtimeClass.isAssignableFrom(clazz)) {
        Some(clazz.asInstanceOf[Class[_ <: T]])
      }
      else None
    }

  }

  object SubClassOf {

    implicit def provide[T: ClassTag]: SubClassOf[T] =
      new SubClassOf[T](implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  }

}
