package reactivemessages.publisher

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import org.scalatest.{Matchers, WordSpecLike}
import reactivemessages.internal.Protocol
import reactivemessages.testkit.sources.NothingSource
import reactivemessages.testkit.subscriber.DummySubscriber

class ReactiveMessagesPublisherSpec extends WordSpecLike with Matchers {

  implicit val system = ActorSystem("TestSystem")

  "ReactiveMessagesPublisher" should {

    "throw an exception if Subscriber is null" in {
      val publisher = new ReactiveMessagesPublisher(NothingSource, TestProbe().ref, system.log)
      intercept[Throwable] { publisher.subscribe(null) }
    }

    "send attach source request to the publisher actor" in {
      val actor = TestProbe()
      val publisher = new ReactiveMessagesPublisher(NothingSource, actor.ref, system.log)
      actor.expectMsg(Protocol.AttachSource(NothingSource))
    }

    "send NewSubscriptionRequest to the underlying publisher processing actor" in {
      val actor = TestProbe()
      val publisher = new ReactiveMessagesPublisher(NothingSource, actor.ref, system.log)
      publisher.subscribe(DummySubscriber)
      actor.fishForMessage() {
        case Protocol.AttachSource(NothingSource) => false
        case Protocol.NewSubscriptionRequest(DummySubscriber) => true
      }
    }

  }

}