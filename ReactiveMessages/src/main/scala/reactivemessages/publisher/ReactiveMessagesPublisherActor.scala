package reactivemessages.publisher

import akka.actor.{Actor, ActorLogging, Props}
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

  override def receive: Receive = awaitingForSource()

  def awaitingForSource(): Receive = {
    /**
     * NOTE :: Source replacement ?? RS Spec ??
     */
    case AttachSource(source) if publisherState.isAwaiting =>
      log.debug(s"Attaching to source [$source]")
      publisherState = State.SourceAttached(source)

      context.become(processMessages())

      /**
       * If we are registering a listener on an active source (that already emits data) then actor publisher
       * starts getting data "as soon as". According to the spec we have to call "onSubscribe" before any other "onX"
       * method.
       */
      source.registerListener(listener)

    case badState =>
      publisherState = State.Crashed(illegalState(State.AwaitingSource))

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
      publisherState match {

        /**
         * NOTE :: Does [[State.AwaitingSource]] makes sense here?
         */
        case State.AwaitingSource | State.SourceAttached(_) =>
          context.actorOf(ReactiveMessagesSubscriptionActor.props(
            subscriber.asInstanceOf[Subscriber[Any]]
          ))

        /**
         * I believe according to the RS spec we still need to call "onSubscribe"
         * right before calling "onComplete". No subscription actor in this case
         */
        case State.SourceDepleted(_) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onComplete()

        case State.Crashed(ex) =>
          subscriber.onSubscribe(EmptySubscription)
          subscriber.onError(ex)
      }

    /**
     * NOTE :: Buffer message if no active subscriptions ?
     */
    case msg @ Protocol.IncomingMessage(message) =>
      context.children.foreach { _ ! msg }

    /**
     * NOTE :: Fault-tolerance strategies ?
     */
    case ex @ Protocol.SourceException(error) =>
      context.children.foreach { _ ! ex }

    /**
     * NOTE :: Alternative / multiple sources ?
     */
    case Protocol.SourceDepleted =>
      publisherState match {
        case State.SourceDepleted(_) =>
          // NOTE :: Is this a valid / possible case ??

        case State.AwaitingSource =>
          publisherState = State.SourceDepleted(None)

        case State.SourceAttached(source) =>
          publisherState = State.SourceDepleted(Some(source))
      }

      // TODO :: RS spec ??
      context.children.foreach { _ ! Protocol.CancelSubscription }
      context.stop(self)
  }

}

object ReactiveMessagesPublisherActor {

  final case class PublisherIllegalState(message: String) extends Throwable(message)


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

    final case class SourceDepleted[Message](
      depletedSource: Option[ReactiveMessagesSource[Message]]
    ) extends State

    final case class Crashed(reason: Throwable) extends State

  }

}