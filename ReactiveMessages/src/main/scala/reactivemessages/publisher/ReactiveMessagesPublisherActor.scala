package reactivemessages.publisher

import akka.actor.{Actor, ActorLogging}
import reactivemessages.sources.ActorListener
import reactivemessages.internal.Protocol
import reactivemessages.internal.Protocol.AttachSource
import reactivemessages.sources.ReactiveMessagesSource

final class ReactiveMessagesPublisherActor extends Actor with ActorLogging {
  import ReactiveMessagesPublisherActor.internal._

  private[this] var internalState: Lifecycle = Lifecycle.AwaitingSource

  val listener = new ActorListener(self)

  override def receive: Receive = awaitingForSource()

  def awaitingForSource(): Receive = {
    case AttachSource(source) =>
      log.debug(s"Attaching to source [$source]")
      internalState = Lifecycle.SourceAttached(source)

      context.become(processMessages())

      /**
       * If we are registering a listener on an active source (that already emits data) then actor publisher
       * starts getting data "as soon as". According to the spec we have to call "onSubscribe" before any other "onX"
       * method.
       */
      source.registerListener(listener)
  }

  def processMessages(): Receive = {
    /**
     * When [[ReactiveMessagesPublisher]] receives a new subscription request from some
     * [[org.reactivestreams.Subscriber]], the publisher signals to the underlying
     * [[ReactiveMessagesPublisherActor]] the subscription request, which in its turn
     * creates a child instance of [[ReactiveTweetSubscriptionActor]] to manage the
     * [[org.reactivestreams.Subscription]] instance.
     */
    case Protocol.NewSubscriptionRequest(subscriber) =>

  }

}

object ReactiveMessagesPublisherActor {

  private object internal {
    sealed trait Lifecycle
    object Lifecycle {
      case object AwaitingSource extends Lifecycle
      final case class SourceAttached[+Message](
        source: ReactiveMessagesSource[Message]
      ) extends Lifecycle
    }
  }

}