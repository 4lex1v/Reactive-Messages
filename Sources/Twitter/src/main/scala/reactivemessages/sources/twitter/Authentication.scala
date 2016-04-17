package reactivemessages.sources
package twitter

final case class Authentication(
  consumerKey:       String = "",
  consumerSecret:    String = "",
  accessTokenKey:    String = "",
  accessTokenSecret: String = ""
)