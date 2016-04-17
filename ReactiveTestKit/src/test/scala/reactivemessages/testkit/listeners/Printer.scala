package reactivemessages.testkit
package listeners

import reactivemessages.sources.{Message, ReactiveMessagesListener}

object Printer extends ReactiveMessagesListener[Any] {

  override def onMessage(message: Message[Any]): Unit = println(message)

  override def onError(error: Throwable): Unit = throw error
}
