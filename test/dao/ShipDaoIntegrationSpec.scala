package dao

import models.{JsonFormats, Point, Ship}
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeApplication
import reactivemongo.core.errors.DatabaseException
import utils.TestUtils._

import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class ShipDaoIntegrationSpec extends Specification {

  val ship = Ship("ship1", 1, 2, 3, Point(0, 0))
  val ship2 = Ship("ship2", 11, 12, 13, Point(5, 5))

  val app = FakeApplication()
  implicit val _ = testEnv(app)

  sequential
  stopOnFail

  import ShipsDao._

  "Ship Dao" should {

    step {
      removeAll().run.force
      ensure(ShipsDao.requiredIndexes).run.force
    }

    "find not existing ship" in {
      findOne(ship.name).run.force === None
    }

    "create a ship" in {
      save(ship).run.force.ok === true
    }

    "find existing ship" in {
      findOne(ship.name).run.force.get === ship
    }

    "fail to create a ship with same name" in {
      save(ship).run.force must throwA[DatabaseException]
    }

    "update a ship" in {
      update(ship.copy(loc = Point(1, 1))).run.force.ok === true
      findOne(ship.name).run.force.get.loc === Point(1, 1)
    }

    "fail to update non-existing ship" in {
      update(ship.copy(name = "wrongName")).run.force.n === 0
    }

    "delete ship" in {
      delete(ship.name).run.force.ok === true
    }

    "delete non-existing ship" in {
      delete(ship.name).run.force.n === 0
    }

    "parse and insert data" in {
      val is = app.resourceAsStream("data/sample_data.json").get

      val ships = JsonFormats.parseSampleData(is)
      ships.length === 296
      Future.sequence(ships.map(save(_).run)).force
      ok
    }

    "find all ships" in {
      findMany().run.force.size === 296
      findMany(itemsPerPageOpt = Option(20)).run.force.size === 20
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(1)).run.force.size === 20
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(15)).run.force.size === 16
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(18)).run.force.size === 0
    }


  }
}