package reactivemessages.sources

import akka.actor.ActorRef
import reactivemessages.internal.Protocol

final class ActorListener[-Message](actor: ActorRef) extends ReactiveMessagesListener[Message] {

  override def onMessage[MT <: Message](message: MT): Unit = {
    actor ! Protocol.IncomingMessage(message)
  }

  override def onError(error: Throwable): Unit = {
    actor ! Protocol.SourceException(error)
  }
}
