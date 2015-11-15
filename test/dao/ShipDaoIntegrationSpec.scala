package dao

import models.{JsonFormats, Location, Ship}
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeApplication
import play.modules.reactivemongo.{DefaultReactiveMongoApi, ReactiveMongoApi}
import reactivemongo.core.errors.DatabaseException
import utils.TestUtils._

import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class ShipDaoIntegrationSpec extends Specification {

  val ship = Ship("ship1", 1, 2, 3, Location(0, 0))
  val ship2 = Ship("ship2", 11, 12, 13, Location(10, 110))

  val app = FakeApplication()

  val dao = new ShipsDao {
    override def reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(app.actorSystem, app.configuration, new DefaultApplicationLifecycle)
  }


  sequential
  stopOnFail

  "Ship Dao" should {

    step {
      dao.removeAll().force
      dao.ensure(ShipsDao.requiredIndexes).force
    }

    "find not existing ship" in {
      dao.findOne(ship.name).force === None
    }

    "create a ship" in {
      dao.save(ship).force.ok === true
    }

    "find existing ship" in {
      dao.findOne(ship.name).force.get === ship
    }

    "fail to create a ship with same name" in {
      dao.save(ship).force must throwA[DatabaseException]
    }

    "update a ship" in {
      dao.update(ship.copy(lastSeen = Location(1, 1))).force.ok === true
      dao.findOne(ship.name).force.get.lastSeen === Location(1, 1)
    }

    "fail to update non-existing ship" in {
      dao.update(ship.copy(name = "wrongName")).force.n === 0
    }

    "delete ship" in {
      dao.delete(ship.name).force.ok === true
    }

    "delete non-existing ship" in {
      dao.delete(ship.name).force.n === 0
    }

    "parse and insert data" in {
      val is = app.resourceAsStream("data/sample_data.json").get

      val ships = JsonFormats.parseSampleData(is)
      ships.length === 296
      Future.sequence(ships.map(dao.save)).force
      ok
    }

    "find all ships" in {
      dao.findMany().force.size === 296
      dao.findMany(itemsPerPageOpt = Option(20)).force.size === 20
      dao.findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(1)).force.size === 20
      dao.findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(15)).force.size === 16
      dao.findMany(itemsPerPageOpt = Option(20), pageNumOpt = Some(18)).force.size === 0
    }


  }
}