import java.util.concurrent.{Future => JavaFuture}
import scala.concurrent.ExecutionContext
import scala.concurrent.{Future => ScalaFuture}

/**
 * You must not use this in production!
 */
object BrokenJavaFutureConverter {
  def toScala[A](jf: JavaFuture[A])(implicit ec: ExecutionContext): ScalaFuture[A] = {
    ScalaFuture(jf.get())
  }

  implicit class RichJavaFuture[A](val jf: JavaFuture[A]) extends AnyVal {
    def asScala(implicit ec: ExecutionContext): ScalaFuture[A] = toScala(jf)
  }
}
