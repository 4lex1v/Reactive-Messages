package reactivemessages.publisher

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.LoggingAdapter
import org.reactivestreams.{Publisher, Subscriber}
import reactivemessages.internal.Protocol
import reactivemessages.sources.ReactiveMessagesSource
import reactivemessages.utils.RSComplianceKit

final class ReactiveMessagesPublisher[Message] private[reactivemessages] (
  source: ReactiveMessagesSource[Message],
  publisherActor: ActorRef,
  log: LoggingAdapter
) extends Publisher[Message] { self =>

  log.info(s"Starting ReactiveMessagesPublisher using source [$source]")
  publisherActor ! Protocol.AttachSource(source)

  /**
   * 1.09 - [[Publisher.subscribe()]] MUST call onSubscribe on the provided Subscriber prior to any other signals to that
   *        Subscriber and MUST return normally, except when the provided Subscriber is null in which case it MUST throw
   *        a [[java.lang.NullPointerException]] to the caller, for all other situations the only legal way to signal
   *        failure (or reject the Subscriber) is by calling onError (after calling onSubscribe).
   *
   * 1.11 - A Publisher MAY support multiple Subscribers and decides whether each Subscription is unicast or multicast.
   *
   * 2.13 - Calling onSubscribe, onNext, onError or onComplete MUST return normally except when any provided parameter
   *        is null in which case it MUST throw a java.lang.NullPointerException to the caller, for all other situations
   *        the only legal way for a Subscriber to signal failure is by cancelling its Subscription. In the case that
   *        this rule is violated, any associated Subscription to the Subscriber MUST be considered as cancelled, and
   *        the caller MUST raise this error condition in a fashion that is adequate for the runtime environment.
   */
  override def subscribe(subscriber: Subscriber[_ >: Message]): Unit = {
    RSComplianceKit.subscriberNotNull(subscriber)

    publisherActor ! Protocol.NewSubscriptionRequest(subscriber)
  }

  override def toString: String = s"ReactiveMessagePublisher[$source]"
}

object ReactiveMessagesPublisher {
  def apply[Message](source: ReactiveMessagesSource[Message])(implicit sys: ActorSystem): ReactiveMessagesPublisher[Message] = {
    new ReactiveMessagesPublisher[Message](source, sys.actorOf(ReactiveMessagesPublisherActor.props()), sys.log)
  }
}