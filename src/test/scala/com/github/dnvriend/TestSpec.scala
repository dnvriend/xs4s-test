package com.github.dnvriend

import java.io.InputStream
import java.net.URL
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import akka.util.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{FlatSpec, Matchers, OptionValues, TryValues}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

trait TestSpec extends FlatSpec with Matchers with ScalaFutures with TryValues with OptionValues with Eventually {
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 1.hour)

  final val TestPartnerFeed = "TestPartnerFeed.xml"

  /**
   * TestKit-based probe which allows sending, reception and reply.
   */
  def probe: TestProbe = TestProbe()

  /**
   * Returns a random UUID
   */
  def randomId = UUID.randomUUID.toString.take(5)

  /**
   * Sends the PoisonPill command to an actor and waits for it to die
   */
  def cleanup(actors: ActorRef*): Unit = {
    actors.foreach { (actor: ActorRef) =>
      actor ! PoisonPill
      probe watch actor
    }
  }

  def withInputStream(fileName: String)(f: InputStream ⇒ Unit): Unit = {
    val is = fromClasspathAsStream(fileName)
    try { f(is) } finally { Try(is.close()) }
  }

  implicit class FutureToTry[T](f: Future[T]) {
    def toTry: Try[T] = Try(f.futureValue)
  }

  implicit class MustBeWord[T](self: T) {
    def mustBe(pf: PartialFunction[T, Unit]): Unit =
      if (!pf.isDefinedAt(self)) throw new TestFailedException("Unexpected: " + self, 0)
  }

  def streamToString(is: InputStream): String =
    Source.fromInputStream(is).mkString

  def fromClasspathAsString(fileName: String): String =
    streamToString(fromClasspathAsStream(fileName))

  def fromClasspathAsStream(fileName: String): InputStream =
    getClass.getClassLoader.getResourceAsStream(fileName)

  def checkConnection(url: String): Future[String] = Future {
    Source.fromInputStream(new URL(url).openConnection().getInputStream).mkString
  }
}