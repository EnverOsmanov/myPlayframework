package yalp.core.server

trait ReloadableServer {

  def stop(): Unit
  def reload(): Unit

  def mainAddress: java.net.InetSocketAddress

}
