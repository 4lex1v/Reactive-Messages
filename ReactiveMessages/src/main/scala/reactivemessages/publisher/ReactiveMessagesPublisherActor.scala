package reactivemessages.publisher

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import org.reactivestreams.Subscriber
import reactivemessages.sources.ActorListener
import reactivemessages.internal.Protocol
import reactivemessages.internal.Protocol.AttachSource
import reactivemessages.sources.ReactiveMessagesSource
import reactivemessages.subscription.{EmptySubscription, ReactiveMessagesSubscriptionActor}

final class ReactiveMessagesPublisherActor extends Actor with ActorLogging {
  import ReactiveMessagesPublisherActor._

  /**
   * Publisher State management.
   * Verbose for testing purposes, any better way to do this?
   */
  private[this] var publisherState: State = State.AwaitingSource
  def transitToState(s: State) = { publisherState = s }
  def currentState: State = publisherState

  val listener = new ActorListener(self)

  def illegalState(expected: State) = PublisherIllegalState(s"PublisherActor state $publisherState, expected $expected")

  override def receive: Receive = LoggingReceive {

    /**
     * NOTE :: Source replacement ?? RS Spec ??
     */
    case AttachSource(source) =>
      publisherState match {

        case State.AwaitingSource =>
          log.debug(s"Attaching to source [$source]")
          publisherState = State.SourceAttached(source)

          /**
           * If we are registering a listener on an active source (that already emits data) then actor publisher
           * starts getting data "as soon as". According to the spec we have to call "onSubscribe" before any other "onX"
           * method.
           *
           * NOTE :: This Publisher won't get any data until we wont register an interest with the Listener
           * TODO :: Ensure that one publisher registers only one listener
           *
           */
          source.registerListener(listener)

        case State.SourceAttached(_) =>
          // TODO :: Source merging ??

        case State.SourceDepleted(depleted) =>
          // TODO :: Are these valid / possible cases ??

        case State.Failed(error) =>
          // TODO :: Fault-tolerance strategy ??
      }

    /**
     * When [[ReactiveMessagesPublisher]] receives a new subscription request from some
     * [[org.reactivestreams.Subscriber]], the publisher signals to the underlying
     * [[ReactiveMessagesPublisherActor]] the subscription request, which in its turn
     * creates a child instance of [[ReactiveTweetSubscriptionActor]] to manage the
     * [[org.reactivestreams.Subscription]] instance.
     */
    case Protocol.NewSubscriptionRequest(subscriber) =>
      publisherState match {

        /**
         * NOTE :: In this case `onSubscribe` would be called from [[ReactiveMessagesSubscriptionActor]]
         */
        case State.AwaitingSource | State.SourceAttached(_) =>
          val subscription = context.actorOf(ReactiveMessagesSubscriptionActor.props())
          subscription ! Protocol.Subscribe(subscriber)

        /**
         * I believe according to the RS spec we still need to call "onSubscribe"
         * right before calling "onComplete". No subscription actor in this case
         */
        case State.SourceDepleted(_) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onComplete()

        case State.Failed(ex) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onError(ex)
      }

    /**
     * NOTE :: Buffer message if no active subscriptions ?
     */
    case msg @ Protocol.IncomingMessage(message) =>
      message match {

        case Some(_) => context.children.foreach { _ forward msg }

        case None =>
          log.debug("Publisher - SourceDepleted")
          publisherState match {
            case State.SourceDepleted(_) =>
            // NOTE :: Is this a valid / possible case ??

            case State.AwaitingSource =>
              publisherState = State.SourceDepleted(None)

            case State.SourceAttached(source) =>
              publisherState = State.SourceDepleted(Some(source))
              context.children.foreach { _ ! msg }

            case State.Failed(ex) =>
            //
          }

      }

    /**
     * NOTE :: Fault-tolerance strategies ?
     * TODO :: Should the source exception be considered as [[State.Failed]] publisher state ?
     */
    case ex @ Protocol.SourceException(error) =>
      context.children.foreach { _ forward ex }

    /**
     * NOTE :: Alternative / multiple sources ?
     */
//    case msg @ Protocol.SourceDepleted =>
//      log.debug("Publisher - SourceDepleted")
//      publisherState match {
//        case State.SourceDepleted(_) =>
//        // NOTE :: Is this a valid / possible case ??
//
//        case State.AwaitingSource =>
//          publisherState = State.SourceDepleted(None)
//
//        case State.SourceAttached(source) =>
//          publisherState = State.SourceDepleted(Some(source))
//          context.children.foreach { _ ! msg }
//
//        case State.Failed(ex) =>
//          //
//      }

  }

}

object ReactiveMessagesPublisherActor {

  final case class PublisherIllegalState(message: String) extends Throwable(message)

  def props(): Props = { Props(new ReactiveMessagesPublisherActor) }

  sealed trait State {
    private def check[S <: State] = this.isInstanceOf[S]

    def isAwaiting: Boolean = this == State.AwaitingSource
    def isAttached: Boolean = this.isInstanceOf[State.SourceAttached[_]]
    def isDepleted: Boolean = this.isInstanceOf[State.SourceDepleted[_]]
  }

  object State {

    case object AwaitingSource extends State

    final case class SourceAttached[Message](
      source: ReactiveMessagesSource[Message]
    ) extends State

    /**
     * NOTE :: Is None a valid case here?
     */
    final case class SourceDepleted[Message](
      depletedSource: Option[ReactiveMessagesSource[Message]]
    ) extends State

    final case class Failed(reason: Throwable) extends State

  }

}