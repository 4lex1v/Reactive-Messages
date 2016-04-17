package reactivemessages.sources

import _root_.twitter4j.{Status, TwitterStream, TwitterStreamFactory}
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

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

  /**
   * Quick Twitter client for playing around in a Scratchpad
   */
  def clientFromConfig(): TwitterStream = {
    val auth = Authentication.fromConfig()
    val config = new ConfigurationBuilder().build()
    val client = new TwitterStreamFactory(config).getInstance()
    client.setOAuthConsumer(auth.consumerKey, auth.consumerSecret)
    client.setOAuthAccessToken(new AccessToken(auth.accessTokenKey, auth.accessTokenSecret))
    client
  }

}
