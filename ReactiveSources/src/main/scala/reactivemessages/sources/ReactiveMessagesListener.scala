package reactivemessages.sources

/**
 * For now exists in order to decouple [[ReactiveMessagesSource]] from
 * underlying Akka based implementation.
 */
trait ReactiveMessagesListener[-MessageType] {

  /**
   * @param message - Message to be sent/processed by the publisher
   */
  def onMessage[MT <: MessageType](message: MT): Unit

  /**
   * In case of any error in the source we need to pass the error downstream to the
   * publisher using this function
   */
  def onError(error: Throwable): Unit

}