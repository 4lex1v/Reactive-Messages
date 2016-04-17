package reactivemessages.internal

import org.reactivestreams.Subscriber
import reactivemessages.sources.ReactiveMessagesSource

private[reactivemessages] object Protocol {

  final case class NewSubscriptionRequest(s: Subscriber[_])

  final case class AttachSource[Message](source: ReactiveMessagesSource[Message])

}