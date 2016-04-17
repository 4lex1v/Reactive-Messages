package reactivemessages

import reactivemessages.testkit.listeners.Printer
import sources.twitter._

object TweetsPrinter extends App {

  ReactiveTweets.stream(Printer)

}
