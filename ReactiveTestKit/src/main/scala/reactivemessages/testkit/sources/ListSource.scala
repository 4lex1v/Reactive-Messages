package reactivemessages.testkit.sources

import reactivemessages.sources.{ReactiveMessagesListener, ReactiveMessagesSource}

final class ListSource[A](list: List[A]) extends ReactiveMessagesSource[A] {
  override def registerListener[MT >: A](listener: ReactiveMessagesListener[MT]): Unit = {
    list.foreach(listener.onMessage)
    listener.onComplete()
  }

  override def toString: String = "ListSource"
}
