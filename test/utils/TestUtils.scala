package utils

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}


object TestUtils {

  implicit val AwaitTimeout = 5.seconds

  implicit class futureToResult[T](f: Future[T]) {
    def force(implicit atMost: Duration): T = Await.result(f, atMost)
  }

}
