package reactivemessages.testkit.sources

import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}

object InfiniteStringSource extends ReactiveMessagesSource[String] {

  private[this] var listener: ReactiveMessagesListener[String] = _
  private[this] var _stop: Boolean = false

  override def registerListener[MT >: String](_listener: ReactiveMessagesListener[MT]): Unit = {
    listener = _listener

    new Thread(new StringEmitter).start()
  }

  def stop() = {
    _stop = true
    listener.onComplete()
  }

  private class StringEmitter extends Runnable {
    override def run(): Unit = {
      while(!_stop) {
        listener.onMessage("TEST STRING")
        Thread.sleep(100)
      }
    }
  }

  override def toString: String = "InfiniteStringSource"
}
