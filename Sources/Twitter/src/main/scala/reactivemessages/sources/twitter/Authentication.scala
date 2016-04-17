package reactivemessages.sources
package twitter

import com.typesafe.config.ConfigFactory

final case class Authentication(
  consumerKey:       String,
  consumerSecret:    String,
  accessTokenKey:    String,
  accessTokenSecret: String
)

object Authentication {
  def fromConfig(): Authentication = {
    val config = ConfigFactory.load().getConfig("twitter")

    Authentication(
      config.getString("consumer.key"),
      config.getString("consumer.secret"),
      config.getString("accessToken.key"),
      config.getString("accessToken.secret")
    )
  }
}