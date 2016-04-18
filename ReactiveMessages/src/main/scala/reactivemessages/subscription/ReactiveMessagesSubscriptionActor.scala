package reactivemessages.subscription

import akka.actor.{Actor, ActorLogging, Props}
import org.reactivestreams.Subscriber
import reactivemessages.internal.Protocol

import scala.collection.immutable.Queue

final class ReactiveMessagesSubscriptionActor extends Actor with ActorLogging {
  import ReactiveMessagesSubscriptionActor._

  private val publisher = context.parent

  /**
   * Subscription state management
   */
  private[this] var subscriptionState: State = State.Idle
  def transitToState(state: State) = { subscriptionState = state }
  def inState(state: State) = { subscriptionState == state }

  /**
   * NOTE :: What's the best way to implement back-pressure control ??
   */
  private[this] var requested: Long = 0
  private[this] var queue: Queue[Any] = Queue.empty

  override def receive: Receive = awaitingSubscription()

  def awaitingSubscription(): Receive = {
    case Protocol.Subscribe(subscriber) =>
      subscriptionState match {
        case State.Idle =>
          try subscriber.onSubscribe(new ReactiveMessagesSubscription(self))
          catch {
            case ex: Throwable =>
              subscriber.onError(ex)
              transitToState(State.Failed(ex))
              context.stop(self)
          }
          finally {
            if (inState(State.Idle)) {
              transitToState(State.Active(subscriber))
              context.become(processingMessages())
            }
          }

        case State.Active(existing) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onError(new IllegalStateException("Already subscribed"))

        case State.Failed(ex) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onError(ex)

        case State.Cancelled =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onComplete()
      }
  }

  def processingMessages(): Receive = {

    /**
     * NOTE ---------------------------
     * 1.09 - Publisher.subscribe MUST call onSubscribe on the provided Subscriber prior to any other signals to that
     *        Subscriber and MUST return normally, except when the provided Subscriber is null in which case it MUST throw
     *        a java.lang.NullPointerException to the caller, for all other situations the only legal way to signal
     *        failure (or reject the Subscriber) is by calling onError (after calling onSubscribe).
     */
    case Protocol.IncomingMessage(status) => //onActive(_.onNext(status))
      subscriptionState match {
        case State.Active(subscriber) =>
          if (requested > 0) {
            subscriber.onNext(status)
            requested -= 1
          } else {
            queue = queue.enqueue(status)
          }
      }

    /** Send as much as we can */
    case m @ Protocol.RequestMore(nrOfElements) =>
      requested += nrOfElements

      /**
       * Check the queue
       */
      subscriptionState match {
        case State.Active(subscriber) =>
          sendFromQueue(subscriber)
      }


    case Protocol.CancelSubscription =>
      subscriptionState match {
        case State.Active(subscriber) =>
          subscriptionState = State.Cancelled
          subscriber.onComplete()
          context.stop(self)
      }

  }

  def sendFromQueue(subscriber: Subscriber[Any]): Unit = {
    def inner(cnt: Long, loop: Queue[Any], acc: List[Any]): (Queue[Any], List[Any]) = {
      if (cnt > 0) {
        loop.dequeueOption match {
          case None => (Queue.empty, acc)
          case Some((elem, next)) => inner(cnt - 1, next, elem :: acc)
        }
      } else (loop, acc)
    }

    inner(requested, queue, List.empty) match {
      case (left, tosend) =>
        queue = left
        requested -= tosend.size
        tosend.foreach(subscriber.onNext)
    }
  }

  def onActive(handle: Subscriber[Any] => Unit): Unit = {
    subscriptionState match {
      case State.Active(subscriber) if requested > 0 => handle(subscriber)
      case _ =>
    }
  }

}

object ReactiveMessagesSubscriptionActor {

  final case class IllegalStateException(reason: String) extends Throwable(reason)

  def props(): Props = Props[ReactiveMessagesSubscriptionActor]

  sealed trait State
  object State {
    case object Idle extends State
    final case class Active[M](subscriber: Subscriber[M]) extends State
    case object Cancelled extends State
    final case class Failed(reason: Throwable) extends State

  }

}