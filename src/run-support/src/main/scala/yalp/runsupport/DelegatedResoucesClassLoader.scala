package yalp.runsupport

import java.net.URL

class DelegatedResourcesClassLoader(name: String, urls: Array[URL], parent: ClassLoader)
  extends NamedURLClassLoader(name, urls, parent) {
  require(parent ne null)

  override def getResources(name: String): java.util.Enumeration[URL] =
    getParent.getResources(name)

}
