package yalp.runsupport.classloader

import java.io.IOException
import java.lang.reflect.{InvocationTargetException, Method}
import java.net.URL
import java.util
import java.util.Collections

import sun.misc.CompoundEnumeration

class DelegatingClassLoader(private val commonLoader: ClassLoader,
                            private val sharedClasses: Array[String],
                            private val buildLoader: ClassLoader,
                            private val applicationClassLoaderProvider: ApplicationClassLoaderProvider
                           ) extends ClassLoader(commonLoader) {

  @throws(classOf[ClassNotFoundException])
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    /*if (sharedClasses.contains(name)) {
      println(s"DelCP: buildLoader ($name, $resolve)")
      buildLoader.loadClass(name)
    }
    else {
      println(s"DelCP: super.loadClass ($name, $resolve)")
      super.loadClass(name, resolve)
    }*/

    buildLoader.loadClass(name)
  }

  @throws(classOf[IOException])
  override def getResource(name: String): URL = {
    val findResource: Method =
      try classOf[ClassLoader].getDeclaredMethod("findResource", classOf[String])
      catch { case e: NoSuchMethodException => throw new IllegalStateException(e) }

    findResource.setAccessible(true)


    Option(applicationClassLoaderProvider.get) match {
      case Some(appClassLoader) =>
        try findResource.invoke(appClassLoader, name).asInstanceOf[URL]
        catch {
          case e@(_: IllegalAccessException | _: InvocationTargetException) => throw new IllegalStateException(e)
        }

      case None => super.getResource(name)
    }
  }

  override def getResources(name: String): util.Enumeration[URL] = {

    val findResouces: Method =
      try classOf[ClassLoader].getDeclaredMethod("findResources", classOf[String])
      catch { case e: NoSuchMethodException => throw new IllegalStateException(e) }

    findResouces.setAccessible(true)
    val appClassLoaderO = Option(applicationClassLoaderProvider.get)

    val resources1 =
      appClassLoaderO match {
        case Some(appClassLoader) =>

          try findResouces.invoke(appClassLoader, name).asInstanceOf[util.Enumeration[URL]]
          catch {
            case e@(_: IllegalAccessException | _: InvocationTargetException) => throw new IllegalStateException(e)
          }

        case None =>
          Collections.emptyEnumeration[URL]()
      }

    val resources2 = super.getResources(name)

    combineResources(resources1, resources2)
  }

  private def combineResources(resources1: util.Enumeration[URL], resources2: util.Enumeration[URL]): util.Enumeration[URL] = {
    new CompoundEnumeration(Array(resources1, resources2))
  }

  override def toString: String =
    s"DelegatingClassLoader, using parent: $getParent"

}
