package reactivemessages.internal

import org.reactivestreams.Subscriber

private[reactivemessages] object Protocol {

  /** Publisher protocol */
  final case class NewSubscriptionRequest(s: Subscriber[_])

}