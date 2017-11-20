package yalp.core.server

import java.util.Properties

trait ServerProcess {

}

class RealServerProcess(val args: Seq[String]) extends ServerProcess {

  def properties: Properties = System.getProperties

}
