package net.fosdal

import net.fosdal.oslo.oany._
import net.fosdal.oslo.oduration._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

package object oslo {

  // TODO use typeclasses for Closer and Logger

  implicit def NoOpCloser[A](a: A): Unit = {}

  implicit def CloseCloser[A <: { def close(): Unit }](a: A): Unit = a.close()

  implicit def StopCloser[A <: { def stop(): Unit }](a: A): Unit = a.stop()

  def using[A, B](resource: A)(f: A => B)(implicit closer: A => Unit): B = {
    try f(resource)
    finally closer(resource)
  }

  def logElapsedTime[Result](logger: (String) => Unit)(block: => Result): Result = {
    val start = System.nanoTime()
    block tap { (_: Result) =>
      val delta = (System.nanoTime() - start).nanoseconds
      logger(s"elapsed time: ${delta.pretty}")
    }
  }

  def sleep(duration: FiniteDuration): Unit = sleep(duration.toMillis)

  def sleep(millis: Long): Unit = Thread.sleep(millis)

  implicit val DefaultConfiguration = UntilConfig(20.seconds, 5.seconds)
  implicit val DefaultLogger        = (_: String) => {}

  def until(block: => Boolean)(implicit config: UntilConfig,
                               logger: (String) => Unit,
                               ec: ExecutionContext): Future[Unit] = {
    sleep(config.initialDelay)
    Future {
      logElapsedTime(logger) {
        while (!block) {
          logger(s"retrying in ${config.delay.prettyAbbr}")
          sleep(config.delay)
        }
      }
    }
  }

  def until(config: UntilConfig)(block: => Boolean)(implicit logger: (String) => Unit,
                                                    ec: ExecutionContext): Future[Unit] = {
    until(block)(config, logger, ec)
  }

  def until(logger: (String) => Unit)(block: => Boolean)(implicit config: UntilConfig,
                                                         ec: ExecutionContext): Future[Unit] = {
    until(block)(config, logger, ec)
  }

  def until(config: UntilConfig)(logger: (String) => Unit)(block: => Boolean)(
      implicit ec: ExecutionContext): Future[Unit] = {
    until(block)(config, logger, ec)
  }

}