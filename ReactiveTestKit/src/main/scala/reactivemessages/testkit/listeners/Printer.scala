package reactivemessages.testkit
package listeners

import reactivemessages.sources.ReactiveMessagesListener

object Printer extends ReactiveMessagesListener[Any] {

  override def onMessage[MT <: Any](message: MT): Unit = println(message)

  override def onError(error: Throwable): Unit = throw error
}
