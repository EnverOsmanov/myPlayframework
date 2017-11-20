package yalp.runsupport.classloader

trait ApplicationClassLoaderProvider {
  def get: ClassLoader
}
