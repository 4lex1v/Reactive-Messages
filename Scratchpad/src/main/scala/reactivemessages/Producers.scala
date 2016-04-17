package reactivemessages

import akka.actor.ActorSystem
import org.reactivestreams.Publisher
import reactivemessages.publisher.ReactiveMessagesPublisher
import reactivemessages.sources.ReactiveMessagesSource
import reactivemessages.testkit.sources.ListSource


object Producers extends App {

  def fromList[A](list: List[A])(implicit sys: ActorSystem): Publisher[A] = {
    ReactiveMessagesPublisher[A](new ListSource[A](list))
  }

  def fromSource[A](source: ReactiveMessagesSource[A])(implicit sys: ActorSystem): Publisher[A] = {
    ReactiveMessagesPublisher[A](source)
  }

}
