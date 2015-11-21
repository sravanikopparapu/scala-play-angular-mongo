package controllers

import dao.ShipsDao
import models.JsonFormats._
import models.{Location, Ship}
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import utils.TestUtils._

@RunWith(classOf[JUnitRunner])
class ShipRestIntegrationSpec extends Specification {

  sequential
  stopOnFail

  val ship = Json.toJson(Ship("ship1", 1, 2, 3, Location(0, 0)))
  val ship_update = Json.toJson(Ship("ship1", 11, 12, 13, Location(10, 110)))

  val dao = testEnv()

  ShipsDao.removeAll()(dao).force

  "Ships API" should {

    "insert a valid json" in {
      running(FakeApplication()) {
        val response = route(FakeRequest(POST, "/ship").withJsonBody(ship))
        response.isDefined === true
        val result = response.get.force
        result.header.status === CREATED
      }
    }

    "fail inserting a non valid json" in {
      running(FakeApplication()) {
        val request = FakeRequest(POST, "/ship").withJsonBody(Json.obj(
          "name" -> "123"))

        val response = route(request)
        response.isDefined === true
        val result = response.get.force
        contentAsString(response.get) === "invalid json"
        result.header.status === BAD_REQUEST
      }
    }

    "update with a valid json" in {
      running(FakeApplication()) {
        val request = FakeRequest(PUT, "/ship").withJsonBody(ship_update)
        val response = route(request)
        response.isDefined === true
        val result = response.get.force
        result.header.status === OK
      }
    }

    /// etc etc etc

  }
}