package dao

import conf.Env
import conf.Env._
import models.Ship
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json._
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.Future


object ShipsDao {

  import models.JsonFormats._

  val Name = "name"

  val requiredIndexes = Seq(
    Index(key = Seq(Name -> IndexType.Ascending), unique = true))

  protected def hasName(name: String) = Json.obj(Name -> name)

  def save(ship: Ship): DI[Future[WriteResult]] = { ctx: Env =>
    ctx.ships.insert(ship)
  }

  def update(ship: Ship): DI[Future[UpdateWriteResult]] = { ctx: Env =>
    ctx.ships.update(selector = hasName(ship.name), update = ship, upsert = false)
  }

  def delete(name: String): DI[Future[WriteResult]] = { ctx: Env =>
    ctx.ships.remove(query = hasName(name), firstMatchOnly = false)
  }

  def findOne(name: String): DI[Future[Option[Ship]]] = { ctx: Env =>
    ctx.ships.find(hasName(name)).one[Ship]
  }

  def findMany(query: JsObject = Json.obj(),
               sort: JsObject = Json.obj(Name -> 1),
               itemsPerPageOpt: Option[Int] = None,
               pageNumOpt: Option[Int] = None): DI[Future[List[Ship]]] = { ctx: Env =>

    val itemsPerPage = itemsPerPageOpt.getOrElse(Int.MaxValue)
    val pageNum = pageNumOpt.getOrElse(1) - 1

    ctx.ships.find(query).options(QueryOpts(skipN = pageNum * itemsPerPage, batchSizeN = itemsPerPage)).
      sort(sort).
      cursor[Ship](ctx.reactiveMongoApi.connection.options.readPreference).
      collect[List](itemsPerPage)
  }

  def countTotal(): DI[Future[Int]] = { ctx: Env =>
    ctx.ships.count()
  }

  def ensure(required: Seq[Index]): DI[Future[Seq[Boolean]]] = { ctx: Env =>
    Future.sequence(required.map(ctx.ships.indexesManager.ensure))
  }

  def removeAll(): DI[Future[WriteResult]] = { ctx: Env =>
    ctx.ships.remove(query = Json.obj(), firstMatchOnly = false)
  }

}
