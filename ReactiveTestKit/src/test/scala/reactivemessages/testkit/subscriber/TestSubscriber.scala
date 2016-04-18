package reactivemessages.testkit.subscriber

import akka.actor.ActorSystem
import org.reactivestreams.{Subscriber, Subscription}
import akka.testkit._

/**
 * Inspired by the test subscriber from akka-stream-test
 */
final class TestSubscriber[M](implicit sys: ActorSystem) extends Subscriber[M] {

  import TestSubscriber._

  private val probe = TestProbe()

  def expectAnySubscription(): Unit = {
    probe.expectMsgType[OnSubscribe]
  }

  def expectSubscription(subs: Subscription): Unit = {
    probe.expectMsg(OnSubscribe(subs))
  }

  def expectComplete(): Unit = {
    probe.expectMsg(OnComplete)
  }

  def expectAnyError(): Unit = {
    probe.expectMsgType[OnError]
  }

  override def onSubscribe(s: Subscription): Unit = probe.ref ! OnSubscribe(s)
  override def onNext(t: M): Unit = probe.ref ! OnNext(t)
  override def onError(t: Throwable): Unit = probe.ref ! OnError(t)
  override def onComplete(): Unit = probe.ref ! OnComplete
}

object TestSubscriber {

  sealed trait SubscriberCommand
  final case class OnNext[M](message: M) extends SubscriberCommand
  final case class OnSubscribe(s: Subscription) extends SubscriberCommand
  final case class OnError(error: Throwable) extends SubscriberCommand
  case object OnComplete extends SubscriberCommand

}
