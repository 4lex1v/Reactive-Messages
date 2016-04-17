package reactivemessages.subscription

import org.reactivestreams.Subscription

private[reactivemessages] object EmptySubscription extends Subscription {
  override def cancel(): Unit = ()
  override def request(n: Long): Unit = ()
}
