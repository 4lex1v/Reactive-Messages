package reactivemessages.sources

import _root_.twitter4j.{Status, TwitterStream}

package object twitter {

  /**
   * Conversion for cleaner User level API
   */
  implicit def streamToReactiveSource(client: TwitterStream): ReactiveMessagesSource[Status] = new ReactiveTweets(client)

  implicit val fromStatusConverter: MessageConverter[Status] = {
    new MessageConverter[Status] {
      override def apply(source: Status): Message[Status] = {
        new Message[Status] {}
      }
    }
  }

}
