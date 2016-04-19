package reactivemessages.subscription

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
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

  override def receive: Receive = LoggingReceive {

    /*******************************************************************************************************************
     *                                                                                                                 *
     *                                           [[Protocol.Subscribe]]                                                *
     *                                                                                                                 *
     ******************************************************************************************************************/

    case Protocol.Subscribe(subscriber) =>
      subscriptionState {
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

    /*******************************************************************************************************************
     *                                                                                                                 *
     *                                           [[Protocol.IncomingMessage]]                                          *
     *                                                                                                                 *
     ******************************************************************************************************************/

    /**
     * NOTE ---------------------------
     * 1.09 - Publisher.subscribe MUST call onSubscribe on the provided Subscriber prior to any other signals to that
     *        Subscriber and MUST return normally, except when the provided Subscriber is null in which case it MUST throw
     *        a java.lang.NullPointerException to the caller, for all other situations the only legal way to signal
     *        failure (or reject the Subscriber) is by calling onError (after calling onSubscribe).
     */
    case Protocol.IncomingMessage(Some(status)) =>
      subscriptionState {
        case State.Active(subscriber) =>
          if (requested > 0) {
            log.debug("Sending incoming message")
            subscriber.onNext(status)
            requested -= 1
          } else {
            log.debug("Buffering incoming message")
            queue = queue.enqueue(status)
          }
      }

    case Protocol.IncomingMessage(None) =>
      if (queue.nonEmpty) {
        import scala.concurrent.duration._
        import context.dispatcher
        context.system.scheduler.scheduleOnce(500.millis, self, Protocol.SourceDepleted)
      } else {
        subscriptionState {
          case State.Active(subscriber) =>
            if (queue.nonEmpty) {
              sendFromQueue(subscriber)
                .foreach(subscriber.onNext)
            }
            subscriber.onComplete()
            subscriptionState = State.Idle
        }
      }

    /*******************************************************************************************************************
     *                                                                                                                 *
     *                                           [[Protocol.RequestMore]]                                              *
     *                                                                                                                 *
     ******************************************************************************************************************/

    case m @ Protocol.RequestMore(nrOfElements) if nrOfElements < 1 =>
      val error = new IllegalArgumentException("Cannot request less then one element. Spec 3.9")
      onActive(_.onError(error))

    /** Send as much as we can */
    case m @ Protocol.RequestMore(nrOfElements) =>
      log.debug(s"More elem requested $nrOfElements, requested - $requested")
      requested += nrOfElements

      /**
       * Check the queue
       */
      if (queue.nonEmpty) {
        log.debug(s"Non Empty Queue: $queue")
        log.debug(s"State :: $subscriptionState")
        subscriptionState {
          case State.Active(subscriber) =>
            log.debug("Active State during request")
            sendFromQueue(subscriber)
              .foreach(subscriber.onNext)
        }
      }

    /*******************************************************************************************************************
     *                                                                                                                 *
     *                                           [[Protocol.SourceDepleted]]                                           *
     *                                                                                                                 *
     ******************************************************************************************************************/

    /**
     * NOTE :: What if we have a Fast Producer, that emits a finite amount of data, and Slow Consumer.
     *         In this case Fast Producer would emit all data
     */
    case Protocol.SourceDepleted =>
      subscriptionState {
        case State.Active(subscriber) =>
          if (queue.nonEmpty) {
            sendFromQueue(subscriber)
              .foreach(subscriber.onNext)
          }
          subscriber.onComplete()
          subscriptionState = State.Idle
      }

    /*******************************************************************************************************************
     *                                                                                                                 *
     *                                         [[Protocol.CancelSubscription]]                                         *
     *                                                                                                                 *
     ******************************************************************************************************************/

    case Protocol.CancelSubscription =>
      subscriptionState {
        case State.Active(subscriber) =>
          subscriptionState = State.Cancelled
          context.stop(self)
      }

  }

  def sendFromQueue(subscriber: Subscriber[Any]): List[Any] = {
    def inner(cnt: Long, loop: Queue[Any], acc: List[Any]): (Queue[Any], List[Any]) = {
      if (cnt > 0) {
        loop.dequeueOption match {
          case None => (Queue.empty, acc)
          case Some((elem, next)) => inner(cnt - 1, next, elem :: acc)
        }
      } else (loop, acc)
    }

    log.info(s"Original :: $queue")
    val (left, toSend) = inner(requested, queue, List.empty)
    log.info(s"Requested :: $requested")
    log.info(s"Left :: $left")
    log.info(s"Send :: $toSend")

    queue = left
    requested -= toSend.size
    log.info(s"After :: $requested")
    toSend
  }

  def onActive(handle: Subscriber[Any] => Unit): Unit = {
    subscriptionState {
      case State.Active(subscriber) => handle(subscriber)
    }
  }

}

object ReactiveMessagesSubscriptionActor {

  final case class IllegalStateException(reason: String) extends Throwable(reason)

  def props(): Props = Props[ReactiveMessagesSubscriptionActor]

  sealed trait State {
    def apply(pf: PartialFunction[State, Unit]) = {
      if (pf.isDefinedAt(this)) {
        pf.apply(this)
      }
    }
  }

  object State {
    case object Idle extends State
    final case class Active[M](subscriber: Subscriber[M]) extends State
    case object Cancelled extends State
    final case class Failed(reason: Throwable) extends State
  }

}