package yalp.core

import java.io.File

trait BuildLink {

  def reload: AnyRef

  def projectPath: File

  def settings: Map[String, String]

  def findSource(className: String, line: Integer): Array[AnyRef]

}
