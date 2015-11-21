package controllers

import javax.inject.{Inject, Singleton}

import dao.{Env, ShipsDao}
import models.JsonFormats._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi}
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class Ships @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller with MongoController with Env {

  import ShipsDao._

  implicit val _ = this

  def createShip = Action.async(parse.json) { request =>
    request.body.validate[Ship].map { ship =>
      save(ship).in map { _ =>
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

      update(ship).in map {
        case result if result.ok && result.n > 0 => Ok
        case result if result.ok && result.n == 0 => Conflict("Nothing to update")
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def deleteShip(name: String) = Action.async { request =>
    delete(name).in map {
      case result if result.ok && result.n > 0 => Ok
      case result if result.ok && result.n == 0 => NoContent
      case result => InternalServerError(result.errmsg.getOrElse(""))
    }
  }

  def findShips(count: Option[Int] = None, page: Option[Int] = None) = Action.async {
    for {
      total <- countTotal().in
      ships <- findMany(itemsPerPageOpt = count, pageNumOpt = page).in
    } yield {
      Ok(Json.obj("total" -> total, "results" -> Json.toJson(ships)))
    }
  }

  def findShip(name: String) = Action.async {
    findOne(name).in map {
      case Some(ship) =>
        Ok(Json.toJson(ship))
      case None =>
        NotFound
    }
  }

}
