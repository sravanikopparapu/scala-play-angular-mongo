package dao

import models.{JsonFormats, Location, Ship}
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

  val ship = Ship("ship1", 1, 2, 3, Location(0, 0))
  val ship2 = Ship("ship2", 11, 12, 13, Location(10, 110))

  val app = FakeApplication()
  implicit val _ = testEnv(app)

  sequential
  stopOnFail

  import ShipsDao._

  "Ship Dao" should {

    step {
      removeAll().in.force
      ensure(ShipsDao.requiredIndexes).in.force
    }

    "find not existing ship" in {
      findOne(ship.name).in.force === None
    }

    "create a ship" in {
      save(ship).in.force.ok === true
    }

    "find existing ship" in {
      findOne(ship.name).in.force.get === ship
    }

    "fail to create a ship with same name" in {
      save(ship).in.force must throwA[DatabaseException]
    }

    "update a ship" in {
      update(ship.copy(lastSeen = Location(1, 1))).in.force.ok === true
      findOne(ship.name).in.force.get.lastSeen === Location(1, 1)
    }

    "fail to update non-existing ship" in {
      update(ship.copy(name = "wrongName")).in.force.n === 0
    }

    "delete ship" in {
      delete(ship.name).in.force.ok === true
    }

    "delete non-existing ship" in {
      delete(ship.name).in.force.n === 0
    }

    "parse and insert data" in {
      val is = app.resourceAsStream("data/sample_data.json").get

      val ships = JsonFormats.parseSampleData(is)
      ships.length === 296
      Future.sequence(ships.map(save(_).in)).force
      ok
    }

    "find all ships" in {
      findMany().in.force.size === 296
      findMany(itemsPerPageOpt = Option(20)).in.force.size === 20
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(1)).in.force.size === 20
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(15)).in.force.size === 16
      findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(18)).in.force.size === 0
    }


  }
}