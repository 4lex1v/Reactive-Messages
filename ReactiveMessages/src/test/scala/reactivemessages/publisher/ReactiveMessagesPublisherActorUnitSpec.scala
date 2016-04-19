package reactivemessages.publisher

import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpecLike}
import akka.testkit._
import org.reactivestreams.{Subscriber, Subscription}
import reactivemessages.internal.Protocol
import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}
import reactivemessages.subscription.EmptySubscription
import reactivemessages.testkit.sources.NothingSource
import reactivemessages.testkit.subscriber.{DummySubscriber, TestSubscriber}

class ReactiveMessagesPublisherActorUnitSpec extends WordSpecLike with Matchers {
  import ReactiveMessagesPublisherActorUnitSpec._

  implicit val system = ActorSystem("testing")

  "PublisherActor" when {

    "created" should {
      "be in AwaitingSource state" in withActor { case (_, actor) =>
        actor.currentState.isAwaiting shouldBe true
      }
    }

    "receives an AttachSource message" should {

      "transit to Crashed state if the Actor is not in AwaitingSource state" in withActor { case (actor, impl) =>
        impl.transitToState(State.SourceAttached(NothingSource))
        actor.receive(Protocol.AttachSource(NothingSource))
        impl.currentState shouldBe a[State.Failed]
      }

      "attach source and register an interest with a listener" in withActor { case (actor, impl) =>
        var listenerRegistered: ReactiveMessagesListener[_] = null

        val testSource = new ReactiveMessagesSource[Any] {
          override def registerListener[MT >: Any](listener: ReactiveMessagesListener[MT]): Unit = {
            listenerRegistered = listener
          }
        }

        actor.receive(Protocol.AttachSource(testSource))

        impl.currentState shouldBe State.SourceAttached(testSource)
        listenerRegistered shouldBe impl.listener
      }

    }

    "receives a subscription request" should {

      "complete subscription if source depleted" in withPActor { case (actor, impl) =>
        impl.transitToState(State.SourceDepleted(Some(NothingSource)))

        val subscriber: TestSubscriber[Any] = new TestSubscriber[Any]()
        actor.receive(Protocol.NewSubscriptionRequest(subscriber))

        subscriber.expectSubscription(EmptySubscription)
        subscriber.expectComplete()
      }

      "notify error state to subscriber" in withPActor { case (actor, impl) =>
        impl.transitToState(State.Failed(new Throwable))

        val subscriber: TestSubscriber[Any] = new TestSubscriber[Any]()
        actor.receive(Protocol.NewSubscriptionRequest(subscriber))

        subscriber.expectSubscription(EmptySubscription)
        subscriber.expectAnyError()
      }

      "create subscription" in withPActor { case (actor, impl) =>
        impl.transitToState(State.SourceAttached(NothingSource))

        val subscriber: TestSubscriber[Any] = new TestSubscriber[Any]()
        actor.receive(Protocol.NewSubscriptionRequest(subscriber))

        subscriber.expectAnySubscription()
        impl.context.children shouldNot be ('empty)
      }

    }

  }

}

object ReactiveMessagesPublisherActorUnitSpec {

  def withActor(f: (TestActorRef[ReactiveMessagesPublisherActor], ReactiveMessagesPublisherActor) => Unit)(implicit sys: ActorSystem): Unit = {
    val actor = TestActorRef[ReactiveMessagesPublisherActor]
    f(actor, actor.underlyingActor)
  }

  def withPActor(f: (TestActorRef[ReactiveMessagesPublisherActor], ReactiveMessagesPublisherActor) => Unit)(implicit sys: ActorSystem): Unit = {
    val actor = TestActorRef[ReactiveMessagesPublisherActor]
    val impl = actor.underlyingActor
    f(actor, impl)
  }
}