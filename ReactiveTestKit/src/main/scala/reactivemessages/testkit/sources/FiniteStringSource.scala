package reactivemessages.testkit.sources

import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}

final case class FiniteStringSource(_elements: Long) extends ReactiveMessagesSource[String] {

  private[this] var listener: ReactiveMessagesListener[String] = _
  private[this] var counter = _elements

  override def registerListener[MT >: String](_listener: ReactiveMessagesListener[MT]): Unit = {
    listener = _listener

    new Thread(new StringEmitter).start()
  }

  private class StringEmitter extends Runnable {
    override def run(): Unit = {
      while(counter > 0) {
        listener.onMessage("Test String")
        Thread.sleep(100)
        counter -= 1
      }
    }
  }

}
