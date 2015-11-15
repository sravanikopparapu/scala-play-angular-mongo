package controllers

import javax.inject.{Inject, Singleton}

import dao.ShipsDao
import models.JsonFormats._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.Future

@Singleton
class Ships @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller with ShipsDao {


  def createShip = Action.async(parse.json) { request =>
    request.body.validate[Ship].map { ship =>
      save(ship) map { _ =>
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def updateShip() = Action.async(parse.json) { request =>
    request.body.validate[Ship].map { ship =>

      update(ship).map {
        case result if result.ok && result.n > 0 => Ok
        case result if result.ok && result.n == 0 => Conflict("Nothing to update")
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def deleteShip(name: String) = Action.async(parse.empty) { request =>
    delete(name) map {
      case result if result.ok && result.n > 0 => Ok
      case result if result.ok && result.n == 0 => NoContent
      case result => InternalServerError(result.errmsg.getOrElse(""))
    }
  }

  def findShips(limit: Option[Int]) = Action.async {

    findMany(limit = limit) map { ships =>
      Ok(Json.arr(ships))
    }
  }

}
