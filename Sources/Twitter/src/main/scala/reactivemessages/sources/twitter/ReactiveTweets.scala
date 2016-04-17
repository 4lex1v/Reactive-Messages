package reactivemessages.sources
package twitter

import twitter4j._

final class ReactiveTweets(client: TwitterStream) extends ReactiveMessagesSource[Status] {

  override def registerListener(listener: ReactiveMessagesListener[Status]): Unit = {
    client.addListener(new ReactiveStatusListener(listener))
  }

}

