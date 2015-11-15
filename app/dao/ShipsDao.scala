package dao

import java.io.{InputStream, File, FileInputStream}

import com.typesafe.scalalogging.slf4j.LazyLogging
import models.JsonFormats._
import models.{Location, Ship}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{DefaultReads, JsArray, JsObject, Json}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.{MongoController, ReactiveMongoComponents}
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.Future
import scala.util.Try

object ShipsDao extends DefaultReads {
  val Name = "name"

  val requiredIndexes = Seq(
    Index(key = Seq(Name -> IndexType.Ascending), unique = true))

  protected def hasName(name: String) = Json.obj(Name -> name)


  def parseSampleData(resource: InputStream): Seq[Ship] = {

    Json.parse(resource).as[JsObject].value("rows").as[JsArray].value.
      map(_.as[JsObject].value).flatMap { v =>
      Try {
        Ship(
          name = v("SHIPNAME").as[String],
          width = v("WIDTH").as[String].toDouble,
          length = v("LENGTH").as[String].toDouble,
          draft = 0,
          lastSeen = Location(
            v("LAT").as[String].toDouble,
            v("LON").as[String].toDouble
          )
        )
      }.toOption
    }
  }

}

trait ShipsDao extends MongoController with ReactiveMongoComponents with LazyLogging {

  import ShipsDao._

  lazy val collection: JSONCollection = db.collection[JSONCollection]("ships")

  def save(ship: Ship): Future[WriteResult] = {
    collection.insert(ship)
  }

  def update(ship: Ship): Future[UpdateWriteResult] = {
    collection.update(selector = hasName(ship.name), update = ship, upsert = false)
  }

  def delete(name: String): Future[WriteResult] = {
    collection.remove(query = hasName(name), firstMatchOnly = false)
  }

  def findOne(name: String): Future[Option[Ship]] = {
    collection.find(hasName(name)).one[Ship]
  }

  def findMany(query: JsObject = Json.obj(),
               sort: JsObject = Json.obj(Name -> 1),
               itemsPerPageOpt: Option[Int] = None,
               pageNumOpt: Option[Int] = None): Future[List[Ship]] = {

    val itemsPerPage = itemsPerPageOpt.getOrElse(Int.MaxValue)
    val pageNum = pageNumOpt.getOrElse(1) - 1

    collection.find(query).options(QueryOpts(skipN = pageNum * itemsPerPage, batchSizeN = itemsPerPage)).
      sort(sort).
      cursor[Ship](connection.options.readPreference).
      collect[List](itemsPerPage)
  }

  def ensure(required: Seq[Index]) = {
    Future.sequence(required.map(collection.indexesManager.ensure))
  }

  def removeAll() = {
    collection.remove(query = Json.obj(), firstMatchOnly = false)
  }

}
