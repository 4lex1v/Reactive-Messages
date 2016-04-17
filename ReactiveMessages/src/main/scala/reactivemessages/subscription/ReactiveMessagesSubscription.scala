package reactivemessages.subscription

import akka.actor.ActorRef
import reactivemessages.internal.Protocol
import org.reactivestreams.Subscription

final class ReactiveMessagesSubscription(actorSubscription: ActorRef) extends Subscription {

  override def cancel(): Unit = { actorSubscription ! Protocol.CancelSubscription }

  override def request(n: Long): Unit = { actorSubscription ! Protocol.RequestMore(n) }
}