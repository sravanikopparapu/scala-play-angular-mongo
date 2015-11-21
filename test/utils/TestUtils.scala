package utils

import env.Env
import play.api.Application
import play.api.inject.DefaultApplicationLifecycle
import play.api.test.FakeApplication
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoApi}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}


object TestUtils {

  implicit val AwaitTimeout = 5.seconds

  implicit class futureToResult[T](f: Future[T]) {
    def force(implicit atMost: Duration): T = Await.result(f, atMost)
  }

  def testEnv(app: Application = FakeApplication()) = {

    new Env {
      override def reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(app.actorSystem, app.configuration, new DefaultApplicationLifecycle)
    }
  }

}
