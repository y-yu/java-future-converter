import java.util.concurrent.ExecutionException
import java.util.concurrent.{Future => JavaFuture}
import scala.concurrent.ExecutionContext
import scala.concurrent.{Future => ScalaFuture}
import scala.util.Failure

object JavaFutureConverter {
  def toScala[A](jf: JavaFuture[A])(implicit ec: ExecutionContext): ScalaFuture[A] = {
    ScalaFuture(jf.get()).transform {
      case Failure(e: ExecutionException) =>
        Failure(e.getCause)
      case x => x
    }
  }

  implicit class RichJavaFuture[A](val jf: JavaFuture[A]) extends AnyVal {
    def asScala(implicit ec: ExecutionContext): ScalaFuture[A] = toScala(jf)
  }
}
