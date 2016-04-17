package reactivemessages.sources.twitter

import reactivemessages.sources.ReactiveMessagesListener
import twitter4j.{Status, StatusAdapter}

final class ReactiveStatusListener(underlying: ReactiveMessagesListener[Status])
  extends StatusAdapter {

  override def onStatus(status: Status): Unit = {
    underlying.onMessage(status)
  }

}
