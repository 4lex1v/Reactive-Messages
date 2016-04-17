package reactivemessages.sources

/**
 * Produces potentially infinite number of messages. Requires an
 * attached [[ReactiveMessagesListener]] to send messages to consumers.
 */
trait ReactiveMessagesSource[+MessageType] {

  /**
   * In order to start consume messages from this source we need to register
   * our interested with this listener.
   *
   * @param listener - Listener interested in received messages from this source
   */
  def registerListener[MT >: MessageType](listener: ReactiveMessagesListener[MT]): Unit
}