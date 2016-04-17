package reactivemessages.internal

import org.reactivestreams.Subscriber
import reactivemessages.sources.ReactiveMessagesSource

private[reactivemessages] object Protocol {

  final case class NewSubscriptionRequest(s: Subscriber[_])

  final case class AttachSource[Message](source: ReactiveMessagesSource[Message])

  final case class IncomingMessage[Message](message: Message)

  final case class SourceException(error: Throwable)

  case object CancelSubscription

  final case class RequestMore(n: Long)

  case object SourceDepleted

}