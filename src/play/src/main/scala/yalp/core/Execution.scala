package yalp.core

import yalp.api.libs.streams.Execution.StreamTrampoline

private[yalp] object Execution {

  def trampoline: StreamTrampoline.type =
    StreamTrampoline

  object Implicits {
    implicit def trampoline: StreamTrampoline.type =
      Execution.trampoline
  }

}
