import java.util.concurrent.ExecutionException
import java.util.concurrent.{Future => JavaFuture}
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.concurrent.{Future => ScalaFuture}
import scala.util.Failure
import scala.util.Try

object JavaFutureConverter {
  def toScala[A](jf: JavaFuture[A])(implicit ec: ExecutionContext): ScalaFuture[A] = {
    val p: Promise[A] = Promise[A]

    ec.execute { () =>
      p.complete(
        Try(jf.get()) match {
          case Failure(e: ExecutionException) =>
            Failure(e.getCause)
          case x =>
            x
        }
      )
    }

    p.future
  }

  implicit class RichJavaFuture[A](val jf: JavaFuture[A]) extends AnyVal {
    def asScala(implicit ec: ExecutionContext): ScalaFuture[A] = toScala(jf)
  }
}
