package yalp.api

class PlayException(title: String, description: String, exception: Throwable = null
                   ) extends UsefulException(title + "[" + description + "]", exception) {

}

object PlayException {

  abstract class ExceptionSource(title: String, description: String, cause: Throwable
                                ) extends PlayException(title, description, cause) {

    def line: Int
    def position: Int
    def input: String
    def sourceName: String

  }
}
