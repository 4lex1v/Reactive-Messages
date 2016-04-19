package reactivemessages.publisher

import akka.actor.ActorSystem
import org.reactivestreams.Publisher
import org.reactivestreams.tck.TestEnvironment
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber
import org.scalatest._
import reactivemessages.publisher.ReactiveMessagesPublisher
import reactivemessages.testkit.RMTestKit
import reactivemessages.testkit.sources.FiniteStringSource

class Publisher101Spec extends WordSpecLike with Matchers with RMTestKit[String] {

  implicit val system = ActorSystem("PublisherSpec")

  "Publisher" should {
    "be good" in {
      activePublisherTest(5, false, { publisher =>
        val sub: ManualSubscriber[String] = env.newManualSubscriber(publisher)

        sub.expectNone(String.format("Publisher %s produced value before the first `request`: ", publisher))
        sub.request(1)
        sub.nextElement(String.format("Publisher %s produced no element after first `request`", publisher))
        sub.expectNone(String.format("Publisher %s produced unrequested: ", publisher))

        sub.request(1)
        sub.request(2)
        sub.nextElements(3, env.defaultTimeoutMillis(), String.format("Publisher %s produced less than 3 elements after two respective `request` calls", publisher))

        sub.expectNone(String.format("Publisher %sproduced unrequested ", publisher))
      })
    }
  }

  override def env: TestEnvironment = new TestEnvironment(500)

  override def createPublisher(nrOrElems: Long): Publisher[String] = {
    val source = new FiniteStringSource(nrOrElems, delay = 200)
    ReactiveMessagesPublisher(source)
  }

}
