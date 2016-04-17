package reactivemessages.subscription

import org.reactivestreams.Subscription

private[reactivemessages] object SourceDepletedSubscription extends Subscription {
  override def cancel(): Unit = ()
  override def request(n: Long): Unit = ()
}
