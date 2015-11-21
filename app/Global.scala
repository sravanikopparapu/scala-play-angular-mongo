import com.typesafe.scalalogging.slf4j.LazyLogging
import env._
import dao.ShipsDao
import dao.ShipsDao._
import models.JsonFormats
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.{Application, GlobalSettings, Mode}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

object Global extends GlobalSettings with LazyLogging {

  override def onStart(app: Application): Unit = {
    if (app.mode == Mode.Dev && app.configuration.getBoolean("shipLocator.bootstrapDataAtStartup").contains(true)) {

      implicit val env = new Env {
        override def reactiveMongoApi: ReactiveMongoApi = Application.instanceCache[ReactiveMongoApi].apply(app)
      }

      Try {
        val is = app.resourceAsStream("data/sample_data.json").get
        JsonFormats.parseSampleData(is)
      } map { ships =>
        for {_ <- removeAll().run
             _ <- ensure(ShipsDao.requiredIndexes).run
        } yield Future.sequence(ships.map(save(_).run))
      } recover {
        case NonFatal(ex) =>
          logger.error(ex.getMessage)
      }
    }
  }
}
