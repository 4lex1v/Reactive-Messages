package reactivemessages.sources

/**
 * TypeClass to capture and convert something into Message
 */
trait MessageConverter[From] {
  def apply(source: From): Message[From]
}