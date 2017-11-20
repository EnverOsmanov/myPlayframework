package yalp.core

import java.io.File

import yalp.api.Application
import yalp.api.mvc.{RequestHeader, Result}

import scala.util.Try

trait ApplicationProvider {

  def get: Try[Application]

  def current: Option[Application] = get.toOption

  def handleWebCommand(requestHeader: RequestHeader): Option[Result] = None

}

trait SourceMapper {

  def sourceOf(className: String, line: Option[Int] = None): Option[(File, Option[Int])]

}
