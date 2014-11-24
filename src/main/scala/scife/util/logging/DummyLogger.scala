package scife.util.logging

import scala.annotation._, elidable._

trait DummyLogger {

  @elidable(SEVERE)
  def severe(msg: => String) = {}

  @elidable(WARNING)
  def error(msg: => String) = {}

  @elidable(WARNING)
  def warning(msg: => String) = {}

  @elidable(INFO)
  def info(msg: => String) = {}

  @elidable(FINE)
  def fine(msg: => String) = {}

  @elidable(FINER)
  def finer(msg: => String) = {}

  @elidable(FINEST)
  def finest(msg: => String) = {}

  @elidable(FINEST)
  def entering(method: => String, arguments: Any*) = {}

  @elidable(FINEST)
  def exiting(method: => String, result: => String) = {}

  @elidable(FINEST)
  def exiting[T](method: => String, result: T): T = result

}
