package reactivemessages.sources
package twitter

import twitter4j._

final class ReactiveTweets(client: TwitterStream) extends ReactiveMessagesSource[Status] {

  override def registerListener[StatusMessage >: Status](listener: ReactiveMessagesListener[StatusMessage]): Unit = {
    client.addListener(new ReactiveStatusListener(listener))
    client.filter(new FilterQuery("scala", "akka", "reactive"))
  }

  override def toString: String = "ReactiveTweets"
}

object ReactiveTweets {
  def stream[StatusMessage >: Status](listener: ReactiveMessagesListener[StatusMessage]): Unit = {
    val client = clientFromConfig()
    new ReactiveTweets(client).registerListener(listener)
    client.sample("en")
  }
}

