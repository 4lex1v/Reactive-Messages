package reactivemessages.utils

import org.reactivestreams.Subscriber

object ReactiveStreamsComplianceKit {

  def subscriberNotNull(subscriber: Subscriber[_]): Unit = {
    if (subscriber eq null) throw null
  }
  
}
