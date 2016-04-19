package reactivemessages.publisher

import akka.actor.ActorSystem
import org.reactivestreams.Publisher
import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.scalatest.testng.TestNGSuiteLike
import reactivemessages.testkit.sources.FiniteStringSource

final class ReactiveMessagesPublisherVerification(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[String](env, publisherShutdownTimeout)
     with TestNGSuiteLike {

//  def this() { this(new TestEnvironment(1500), 2000) }
//  def this() { this(new TestEnvironment(1000), 1500) }
  def this() { this(new TestEnvironment(500), 1000) }

  implicit val system = ActorSystem("ReactiveMessagesTCK")

//  override def skipStochasticTests(): Boolean = true

  override def createPublisher(elements: Long): Publisher[String] = {
    val listSource = new FiniteStringSource(elements, delay = 200)
    ReactiveMessagesPublisher(listSource)
  }

  override def createFailedPublisher(): Publisher[String] = null
}
