package reactivemessages.publisher

import akka.actor.{Actor, ActorLogging, Props}
import org.reactivestreams.Subscriber
import reactivemessages.sources.ActorListener
import reactivemessages.internal.Protocol
import reactivemessages.internal.Protocol.AttachSource
import reactivemessages.sources.ReactiveMessagesSource
import reactivemessages.subscription.{ReactiveMessagesSubscriptionActor, SourceDepletedSubscription}

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
      internalState match {

        /**
         * NOTE :: Does [[Lifecycle.AwaitingSource]] makes sense here?
         */
        case Lifecycle.AwaitingSource | Lifecycle.SourceAttached(_) =>
          context.actorOf(ReactiveMessagesSubscriptionActor.props(
            subscriber.asInstanceOf[Subscriber[Any]]
          ))

        /**
         * I believe according to the RS spec we still need to call "onSubscribe"
         * right before calling "onComplete". No subscription actor in this case
         */
        case Lifecycle.SourceDepleted(_) =>
          subscriber.onSubscribe(SourceDepletedSubscription)
          subscriber.onComplete()
      }

    case msg @ Protocol.IncomingMessage(message) =>
      context.children.foreach { _ ! msg }

    case ex @ Protocol.SourceException(error) =>
      context.children.foreach { _ ! ex }

    case Protocol.SourceDepleted =>
      internalState match {
        case Lifecycle.SourceDepleted(_) =>
          // NOTE :: Is this a valid / possible case ??

        case Lifecycle.AwaitingSource =>
          internalState = Lifecycle.SourceDepleted(None)

        case Lifecycle.SourceAttached(source) =>
          internalState = Lifecycle.SourceDepleted(Some(source))
      }

      // TODO :: RS spec ??
      context.children.foreach { _ ! Protocol.CancelSubscription }
      context.stop(self)


  }

}

object ReactiveMessagesPublisherActor {

  private object internal {
    sealed trait Lifecycle
    object Lifecycle {

      case object AwaitingSource extends Lifecycle

      final case class SourceAttached[Message](
        source: ReactiveMessagesSource[Message]
      ) extends Lifecycle

      final case class SourceDepleted[Message](
        depletedSource: Option[ReactiveMessagesSource[Message]]
      ) extends Lifecycle

    }
  }

}