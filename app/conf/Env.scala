package conf

import play.modules.reactivemongo.ReactiveMongoComponents
import play.modules.reactivemongo.json.collection.JSONCollection

object Env {

  type DI[A] = Env => A

  implicit class DiOpts[A](op: DI[A]) {
    def run(implicit env: Env): A = {
      op.apply(env)
    }
  }

}

trait Env extends ReactiveMongoComponents {
  lazy val ships: JSONCollection = reactiveMongoApi.db.collection[JSONCollection]("ships")
}



