package reactivemessages.testkit.subscriber

import org.reactivestreams.{Subscriber, Subscription}

final class SimpleSubscriber[Message](
  _onNext: Message => Unit,
  _onComplete: () => Unit
) extends Subscriber[Message] {
  override def onError(t: Throwable): Unit = ()

  override def onSubscribe(s: Subscription): Unit = ()

  override def onNext(t: Message): Unit = _onNext(t)

  override def onComplete(): Unit = _onComplete()

  override def toString: String = "SimpleSubscriber"
}
