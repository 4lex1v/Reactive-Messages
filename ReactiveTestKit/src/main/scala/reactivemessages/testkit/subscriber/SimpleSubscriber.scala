package reactivemessages.testkit.subscriber

import java.util.concurrent.atomic.AtomicBoolean

import org.reactivestreams.{Subscriber, Subscription}

final class SimpleSubscriber[Message](
  /**
   * Each Delay millisecond make a request
   */
  delay: Long,

  /**
   * Number of elements to request on each cycle
   */
  request: Long,

  /**
   * Callbacks
   */
  _onNext: Message => Unit,
  _onComplete: () => Unit
) extends Subscriber[Message] { subs =>
  override def onError(t: Throwable): Unit = ()

  private[this] var stop: AtomicBoolean = new AtomicBoolean(false)

  override def onSubscribe(s: Subscription): Unit = {
    new Thread(new Loop(s)).start()
  }

  override def onNext(t: Message): Unit = _onNext(t)

  override def onComplete(): Unit = {
    stop.compareAndSet(false, true)
    _onComplete()
  }

  override def toString: String = "SimpleSubscriber"

  private final class Loop(s: Subscription) extends Runnable {
    override def run(): Unit = {

      while(!stop.get()) {
        s.request(request)
        Thread.sleep(delay)
      }

    }
  }

}
