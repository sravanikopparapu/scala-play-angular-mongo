package controllers

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.slf4j.LazyLogging
import env._
import dao.ShipsDao
import models.JsonFormats._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class Ships @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with ReactiveMongoComponents with Env with LazyLogging {

  import ShipsDao._

  implicit val env = this

  def createShip = Action.async(parse.json) { request =>
    request.body.validate[Ship].map { ship =>
      save(ship).run map { _ =>
        Created
      } recover {
        case e: DatabaseException if e.getMessage().contains("E11000 duplicate key") =>
          BadRequest("Ship with same name already exists")
        case NonFatal(e) =>
          InternalServerError(e.getMessage)
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def updateShip() = Action.async(parse.json) { request =>
    request.body.validate[Ship].map { ship =>

      update(ship).run map {
        case result if result.ok && result.n > 0 => Ok
        case result if result.ok && result.n == 0 => Conflict("Nothing to update")
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def deleteShip(name: String) = Action.async { request =>
    delete(name).run map {
      case result if result.ok && result.n > 0 => Ok
      case result if result.ok && result.n == 0 => NoContent
      case result => InternalServerError(result.errmsg.getOrElse(""))
    }
  }

  def findShips(count: Option[Int] = None, page: Option[Int] = None) = Action.async {
    for {
      total <- countTotal().run
      ships <- findMany(itemsPerPageOpt = count, pageNumOpt = page).run
    } yield {
      Ok(Json.obj("total" -> total, "results" -> Json.toJson(ships)))
    }
  }

  def findShip(name: String) = Action.async {
    findOne(name).run map {
      case Some(ship) =>
        Ok(Json.toJson(ship))
      case None =>
        NotFound
    }
  }

}
