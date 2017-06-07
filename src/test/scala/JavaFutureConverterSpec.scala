import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.{Future => JavaFuture}
import exception.TestException
import org.scalatest.Matchers
import org.scalatest.WordSpec
import util.ExecutorFromExecutorService
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

class JavaFutureConverterSpec extends WordSpec with Matchers {
  import JavaFutureConverter._

  trait SetupWithFixedThreadPool {
    val timeout = Duration(1, TimeUnit.SECONDS)

    val threadPool: ExecutorService = Executors.newFixedThreadPool(1)

    val executor: Executor = new ExecutorFromExecutorService(threadPool)

    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
  }

  trait SetupWithForkJoinPool {
    val timeout = Duration(1, TimeUnit.SECONDS)

    val forkJoinPool: ExecutorService = ForkJoinPool.commonPool()

    val executor: Executor = new ExecutorFromExecutorService(forkJoinPool)

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

    "be able to recover the exception" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Int] = threadPool.submit { () =>
        throw new TestException()
      }

      val recover = javaFuture.asScala.recover {
        case e: TestException => 10
      }

      assert(Await.result(recover, timeout) == 10)
    }

    "return an exception if the java future is canceled" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Unit] = threadPool.submit { () =>
        Thread.sleep(5000)
      }
      javaFuture.cancel(true)

      assertThrows[CancellationException](Await.result(toScala(javaFuture), timeout))
    }

    "return the exception which is inherit RuntimeException if the java future returns it" in new SetupWithFixedThreadPool {
      val javaFuture: JavaFuture[Unit] = threadPool.submit { () =>
        throw new IOException()
      }

      assertThrows[IOException](Await.result(toScala(javaFuture), timeout))
    }

    "return RuntimeException despite it returns IOException if you use the ForkJoinPool executor" in new SetupWithForkJoinPool {
      val javaFuture: JavaFuture[Unit] = forkJoinPool.submit { () =>
        throw new IOException()
      }

      assertThrows[RuntimeException](Await.result(javaFuture.asScala, timeout))
    }
  }
}
