package reactivemessages.testkit

import org.reactivestreams.Publisher
import org.reactivestreams.tck.TestEnvironment
import org.testng.SkipException

trait RMTestKit[T] {

  def env: TestEnvironment

  def createPublisher(nrOrElems: Long): Publisher[T]

  def activePublisherTest(elements: Long, completionSignalRequired: Boolean, test: Publisher[T] => Unit): Unit = {
    test(createPublisher(elements))
    env.verifyNoAsyncErrorsNoDelay()
  }

}
