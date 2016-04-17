package reactivemessages.testkit.sources

import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}

object NothingSource extends ReactiveMessagesSource[Nothing] {
  override def registerListener[MT >: Nothing](listener: ReactiveMessagesListener[MT]): Unit = ()
}
