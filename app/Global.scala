import com.typesafe.scalalogging.slf4j.LazyLogging
import dao.ShipsDao
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

      lazy val dao = new ShipsDao {
        override def reactiveMongoApi: ReactiveMongoApi = Application.instanceCache[ReactiveMongoApi].apply(app)
      }

      Try {
        val is = app.resourceAsStream("data/sample_data.json").get
        JsonFormats.parseSampleData(is)
      } map { ships =>
        for {_ <- dao.removeAll()
             _ <- dao.ensure(ShipsDao.requiredIndexes)
        } yield Future.sequence(ships.map(dao.save))
      } recover {
        case NonFatal(ex) =>
          logger.error(ex.getMessage)
      }
    }
  }
}
