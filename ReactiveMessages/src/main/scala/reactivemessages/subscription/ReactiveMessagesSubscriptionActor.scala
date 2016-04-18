package reactivemessages.subscription

import akka.actor.{Actor, ActorLogging, Props}
import org.reactivestreams.Subscriber
import reactivemessages.internal.Protocol

final class ReactiveMessagesSubscriptionActor (s: Subscriber[Any]) extends Actor with ActorLogging {
  import ReactiveMessagesSubscriptionActor._

  private val publisher = context.parent

  private[this] var subscriptionState: State = _
  private[this] var requested: Long = 0

  /**
   * 1.09 - Publisher.subscribe MUST call onSubscribe on the provided Subscriber prior to any other signals to that
   *        Subscriber and MUST return normally, except when the provided Subscriber is null in which case it MUST throw
   *        a java.lang.NullPointerException to the caller, for all other situations the only legal way to signal
   *        failure (or reject the Subscriber) is by calling onError (after calling onSubscribe).
   */
  override def preStart(): Unit = {
    log.info("Initiating a new Subscription actor")
    try s.onSubscribe(new ReactiveMessagesSubscription(self))
    catch { case ex: Throwable => subscriptionState = State.Suspended(ex) }
    finally {
      if (!subscriptionState.isInstanceOf[State.Suspended]) {
        subscriptionState = State.Active
      }
    }
  }

  override def receive: Receive = {

    /**
     * TODO :: Back-pressure
     * For now just dummy implementation for experimenting with API
     */
    case Protocol.IncomingMessage(status) if subscriptionState.isActive =>
      s.onNext(status)

    //      /**
    //       * This is an interesting case how can we implementation back-pressure according to the
    //       * reactive streams specification.
    //       */
    //      if (requested > 0) {
    //        s.onNext(status)
    //        requested -= 1
    //      }

    //      context.become(processing(statuses.enqueue(status)))

    case Protocol.CancelSubscription if subscriptionState.isActive =>
      subscriptionState = State.Cancelled
      s.onComplete()
      context.stop(self)
    //        publisher ! Protocol.SubscriptionCanceled(self, s)

    /** Send as much as we can */
    case m @ Protocol.RequestMore(nrOfElements) if subscriptionState.isActive =>
      println(s"Subscriber RequestMore :: $nrOfElements")
      requested += nrOfElements

  }

}

object ReactiveMessagesSubscriptionActor {

  def props(subscriber: Subscriber[Any]): Props = {
    Props(new ReactiveMessagesSubscriptionActor(subscriber))
  }

  sealed trait State {
    def isActive: Boolean = { this == State.Active }
  }
  object State {

    case object Active extends State
    case object Cancelled extends State
    final case class Suspended(reason: Throwable) extends State

  }

}