package reactivemessages.testkit.sources

import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}

final case class FiniteStringSource(
  nrOfElements: Long,
  delay: Long = 100
) extends ReactiveMessagesSource[String] {

  override def registerListener[MT >: String](listener: ReactiveMessagesListener[MT]): Unit = {
    new Thread(new StringEmitter(listener)).start()
  }

  private final class StringEmitter (
    listener: ReactiveMessagesListener[String]
  ) extends Runnable {

    override def run(): Unit = {
      /**
       * Send messages to the Publisher via Listener
       */
      (1L to nrOfElements).foreach { idx =>
        listener.onMessage("Test Message String")
        Thread.sleep(delay)
      }

      /**
       * Notify of source depletion
       */
      listener.onComplete()
    }

  }

}
