package reactivemessages

import akka.actor.ActorSystem
import reactivemessages.testkit.sources.{FiniteStringSource, InfiniteStringSource}
import reactivemessages.testkit.subscriber.SimpleSubscriber

object common {
  implicit val system = ActorSystem("ListIteration")

  /**
   * Slow Consumer
   *   delay = 1000
   *
   * Fast Consumer
   *   delay = 0
   */
  val logAndTerminate = new SimpleSubscriber[String](
    delay = 200,
    request = 1,
    msg => println(s"Got Message: $msg"),
    () => { println("Done!"); system.shutdown() }
  )
}

import reactivemessages.common._

object ListIteration extends App {

  // NOTE :: Extremely fast Producer
  Producers.fromList(List("one", "two", "three")).subscribe(logAndTerminate)

}

object FiniteStringPrinter extends App {
  Producers.fromSource(new FiniteStringSource(10)).subscribe(logAndTerminate)
}

object InfiniteStringPrinter extends App {

  Producers.fromSource(InfiniteStringSource).subscribe(logAndTerminate)

}
