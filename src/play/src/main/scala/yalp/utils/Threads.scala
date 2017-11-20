package yalp.utils

object Threads {

  def withContextClassLoader[T](classLoader: ClassLoader)(b: => T): T = {
    val thread = Thread.currentThread()
    val oldLoader = thread.getContextClassLoader

    try {
      thread.setContextClassLoader(classLoader)
      b
    }
    finally thread.setContextClassLoader(oldLoader)
  }

}
