package reactivemessages.publisher

import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpecLike}
import akka.testkit._
import reactivemessages.internal.Protocol
import reactivemessages.publisher.ReactiveMessagesPublisherActor.State
import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}
import reactivemessages.testkit.sources.NothingSource

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
        impl.currentState shouldBe a[State.Crashed]
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
    
  }

}

object ReactiveMessagesPublisherActorUnitSpec {
  def withActor(f: (TestActorRef[ReactiveMessagesPublisherActor], ReactiveMessagesPublisherActor) => Unit)(implicit sys: ActorSystem): Unit = {
    val actor = TestActorRef[ReactiveMessagesPublisherActor]
    f(actor, actor.underlyingActor)
  }
}