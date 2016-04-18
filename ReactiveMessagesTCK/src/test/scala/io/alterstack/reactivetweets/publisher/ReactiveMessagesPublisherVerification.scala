package io.alterstack.reactivetweets.publisher

import akka.actor.ActorSystem
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.scalatest.testng.TestNGSuiteLike
import reactivemessages.publisher.ReactiveMessagesPublisher
import reactivemessages.testkit.sources.FiniteStringSource

final class ReactiveMessagesPublisherVerification(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[String](env, publisherShutdownTimeout)
     with TestNGSuiteLike {

  def this() { this(new TestEnvironment(500), 1000) }

  implicit val system = ActorSystem("ReactiveMessagesTCK")

  override def createPublisher(elements: Long): Publisher[String] = {
    val listSource = new FiniteStringSource(elements)
    ReactiveMessagesPublisher(listSource)
  }

  override def createFailedPublisher(): Publisher[String] = null
}
