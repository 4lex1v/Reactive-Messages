package reactivemessages.sources

/**
 * Generic type of messages flowing through the system.
 * NOTE :: Most likely will be changed a lot.
 */
private[reactivemessages] trait Message[MessageType]

object Message {
  /**
   * To simplify conversion from type `A` into `Message[A]`.
   *
   * Magnet Pattern?
   */
  implicit def apply[A](from: A)(implicit conv: MessageConverter[A]): Message[A] = conv(from)
}