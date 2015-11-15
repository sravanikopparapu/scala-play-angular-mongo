package dao

import com.typesafe.scalalogging.slf4j.LazyLogging
import models.JsonFormats._
import models.Ship
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.{MongoController, ReactiveMongoComponents}
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.Future

object ShipsDao {
  val Name = "name"

  val requiredIndexes = Seq(
    Index(key = Seq(Name -> IndexType.Ascending), unique = true))

  protected def hasName(name: String) = Json.obj(Name -> name)

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
               limit: Option[Int] = None): Future[List[Ship]] = {
    collection.find(query).
      sort(sort).
      cursor[Ship](connection.options.readPreference).
      collect[List](limit.getOrElse(Int.MaxValue))
  }

  def ensure(required: Seq[Index]) = {
    Future.sequence(required.map(collection.indexesManager.ensure))
  }

  def removeAll() = {
    collection.remove(query = Json.obj(), firstMatchOnly = false)
  }

}
