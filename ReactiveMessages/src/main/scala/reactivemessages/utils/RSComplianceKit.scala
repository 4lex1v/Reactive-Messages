package reactivemessages.utils

import org.reactivestreams.Subscriber

object RSComplianceKit {

  def subscriberNotNull(subscriber: Subscriber[_]): Unit = {
    if (subscriber eq null) throw null
  }
  
}
