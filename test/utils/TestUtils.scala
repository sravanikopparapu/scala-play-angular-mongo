package utils

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.io.Source


object TestUtils {

  implicit val AwaitTimeout = 5.seconds

  implicit class futureToResult[T](f: Future[T]) {
    def force(implicit atMost: Duration): T = Await.result(f, atMost)
  }

  def getResource(filename: String): String = {
    val resource = ClassLoader.getSystemResourceAsStream(filename)
    Source.fromInputStream(resource).getLines().mkString(MiscUtils.LineSeparator)
  }

}
