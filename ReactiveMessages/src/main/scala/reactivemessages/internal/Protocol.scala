package reactivemessages.internal

import akka.actor.DeadLetterSuppression
import org.reactivestreams.Subscriber
import reactivemessages.sources.ReactiveMessagesSource

private[reactivemessages] object Protocol {

  final case class NewSubscriptionRequest(s: Subscriber[_])

  final case class AttachSource[Message](source: ReactiveMessagesSource[Message])

  final case class IncomingMessage[Message](message: Message)

  final case class SourceException(error: Throwable)

  case object SourceDepleted

  /**
   * Subscription Protocol
   *
   * Extends [[DeadLetterSuppression]] to avoid unnecessary logging when subscription has been
   * canceled, but the actor still can receive Cancel / Request messages to suffice 3.06 / 3.07
   */
  case object CancelSubscription extends DeadLetterSuppression
  final case class RequestMore(n: Long) extends DeadLetterSuppression

}