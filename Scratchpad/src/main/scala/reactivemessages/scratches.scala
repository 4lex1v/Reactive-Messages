package reactivemessages

import akka.actor.ActorSystem
import reactivemessages.testkit.subscriber.SimpleSubscriber
import reactivemessages.testkit.sources.{FiniteStringSource, InfiniteStringSource}

object common {
  implicit val system = ActorSystem("ListIteration")

  val logAndTerminate = new SimpleSubscriber[String](
    msg => println(s"Got Message: $msg"),
    () => { println("Done!"); system.shutdown() }
  )
}

import common._

object ListIteration extends App {

  Producers.fromList(List("one", "two", "three")).subscribe(logAndTerminate)

}

object FiniteStringPrinter extends App {
  Producers.fromSource(new FiniteStringSource(10)).subscribe(logAndTerminate)
}

object InfiniteStringPrinter extends App {

  Producers.fromSource(InfiniteStringSource).subscribe(logAndTerminate)

}
