package net.fosdal

import net.fosdal.oslo.oany._
import net.fosdal.oslo.oduration._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source.fromInputStream
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

  def time[A](block: => A)(f: FiniteDuration => Unit): A = {
    val start = System.nanoTime()
    val a     = block
    f((System.nanoTime() - start).nanos)
    a
  }

  def fileContents(resource: String): String = {
    // TODO convert to reading file as fall back if resource does not exist
    Option(getClass.getResourceAsStream(s"/$resource")).map(fromInputStream) match {
      case Some(source) => using(source)(_.buffered.mkString)
      case _            => throw new Exception(s"resource not found: $resource")
    }
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

  def until(block: => Boolean)(implicit config: UntilConfig, ec: ExecutionContext): Future[Unit] = {
    sleep(config.initialDelay)
    Future {
      while (!block) {
        sleep(config.delay)
      }
    }
  }

  def until(config: UntilConfig)(block: => Boolean)(implicit ec: ExecutionContext): Future[Unit] = {
    until(block)(config, ec)
  }

}
