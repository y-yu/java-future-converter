import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.{Future => JavaFuture}
import exception.TestException
import org.scalatest.WordSpec
import util.ExecutorFromExecutorService
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

class BrokenJavaFutureConverterSpec extends WordSpec {
  import BrokenJavaFutureConverter._

  trait SetupWithFixedThreadPool {
    val timeout = Duration(1, TimeUnit.SECONDS)

    val threadPool: ExecutorService = Executors.newFixedThreadPool(1)

    val executor: Executor = new ExecutorFromExecutorService(threadPool)

    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
  }

  "toScala" should {
    "return the value" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Int] = threadPool.submit { () =>
        Thread.sleep(200)
        10
      }

      assert(Await.result(toScala(javaFuture), timeout) == 10)
    }

    "return an TimeoutException if the future does not complete in a second" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Unit] = threadPool.submit { () =>
        Thread.sleep(5000)
      }

      assertThrows[TimeoutException](Await.result(javaFuture.asScala, timeout))
    }

    "not be able to recover the exception" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Int] = threadPool.submit{ () =>
        throw new TestException()
      }

      val recover = javaFuture.asScala.recover {
        case e: TestException => 10
      }

      assertThrows[ExecutionException](Await.result(recover, timeout))
    }
  }
}
