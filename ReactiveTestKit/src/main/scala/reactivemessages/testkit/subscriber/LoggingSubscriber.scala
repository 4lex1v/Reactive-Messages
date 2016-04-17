package reactivemessages.testkit.subscriber

import org.reactivestreams.{Subscriber, Subscription}

object LoggingSubscriber extends Subscriber[Any] {
  override def onError(t: Throwable): Unit = ()

  override def onSubscribe(s: Subscription): Unit = ()

  override def onComplete(): Unit = ()

  override def onNext(t: Any): Unit = println(t)
}
