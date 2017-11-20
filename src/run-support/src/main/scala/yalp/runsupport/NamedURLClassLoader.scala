package yalp.runsupport

import java.net.{URL, URLClassLoader}

/**
  * A ClassLoader with a toString() that prints name/urls.
  */
class NamedURLClassLoader(name: String, urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {

  override def toString: String = {
    val urls = getURLs
      .map(_.toString)
      .mkString(", ")

    s"$name{$urls}"
  }

}
