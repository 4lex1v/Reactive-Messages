package reactivemessages

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
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
  def logAndTerminate[A] = new SimpleSubscriber[A](
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

object TwitterStream extends App {
  import reactivemessages.sources.twitter._

  implicit val mat = ActorMaterializer()

  val publisher = Producers.fromSource(new ReactiveTweets(clientFromConfig()))

//  // Custom
//  publisher.subscribe(logAndTerminate)

  // Akka Streams
  Source.fromPublisher(publisher).runForeach(x => println(x.getText))
}