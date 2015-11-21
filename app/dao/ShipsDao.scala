package dao

import models.Ship
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponents
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.Future
import scalaz.Reader


trait Env extends ReactiveMongoComponents {

  lazy val ships: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("ships")

}

object ShipsDao {

  import models.JsonFormats._

  // injects implicit context into reader
  implicit class ReaderOpts[A, ENV](reader: Reader[ENV, A]) {
    def in(implicit env: ENV): A = {
      reader.apply(env)
    }
  }

  val Name = "name"

  val requiredIndexes = Seq(
    Index(key = Seq(Name -> IndexType.Ascending), unique = true))

  protected def hasName(name: String) = Json.obj(Name -> name)

  def save(ship: Ship): Reader[Env, Future[WriteResult]] = Reader { dbs: Env =>
    dbs.ships.insert(ship)
  }

  def update(ship: Ship): Reader[Env, Future[UpdateWriteResult]] = Reader { dbs: Env =>
    dbs.ships.update(selector = hasName(ship.name), update = ship, upsert = false)
  }

  def delete(name: String): Reader[Env, Future[WriteResult]] = Reader { dbs: Env =>
    dbs.ships.remove(query = hasName(name), firstMatchOnly = false)
  }

  def findOne(name: String): Reader[Env, Future[Option[Ship]]] = Reader { dbs: Env =>
    dbs.ships.find(hasName(name)).one[Ship]
  }

  def findMany(query: JsObject = Json.obj(),
               sort: JsObject = Json.obj(Name -> 1),
               itemsPerPageOpt: Option[Int] = None,
               pageNumOpt: Option[Int] = None): Reader[Env, Future[List[Ship]]] = Reader { dbs: Env =>

    val itemsPerPage = itemsPerPageOpt.getOrElse(Int.MaxValue)
    val pageNum = pageNumOpt.getOrElse(1) - 1

    dbs.ships.find(query).options(QueryOpts(skipN = pageNum * itemsPerPage, batchSizeN = itemsPerPage)).
      sort(sort).
      cursor[Ship](dbs.reactiveMongoApi.connection.options.readPreference).
      collect[List](itemsPerPage)
  }

  def countTotal(): Reader[Env, Future[Int]] = Reader { dbs: Env =>
    dbs.ships.count()
  }

  def ensure(required: Seq[Index]): Reader[Env, Future[Seq[Boolean]]] = Reader { dbs: Env =>
    Future.sequence(required.map(dbs.ships.indexesManager.ensure))
  }

  def removeAll(): Reader[Env, Future[WriteResult]] = Reader { dbs: Env =>
    dbs.ships.remove(query = Json.obj(), firstMatchOnly = false)
  }

}
