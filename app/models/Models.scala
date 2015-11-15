package models

import java.io.InputStream

import play.api.libs.json.{DefaultReads, JsArray, JsObject}

import scala.util.Try

case class Ship(name: String,
                width: Double, //in meters
                length: Double, //in meters
                draft: Double, //in meters
                lastSeen: Location
               )

case class Location(lon: Double, lat: Double)

object JsonFormats extends DefaultReads {

  import play.api.libs.json.Json

  implicit val locFormat = Json.format[Location]

  implicit val shipFormat = Json.format[Ship]

  def parseSampleData(resource: InputStream): Seq[Ship] = {

    Json.parse(resource).as[JsObject].value("rows").as[JsArray].value.
      map(_.as[JsObject].value).flatMap { v =>
      Try {
        Ship(
          name = v("SHIPNAME").as[String],
          width = v("WIDTH").as[String].toDouble,
          length = v("LENGTH").as[String].toDouble,
          draft = 0,
          lastSeen = Location(
            v("LAT").as[String].toDouble,
            v("LON").as[String].toDouble
          )
        )
      }.toOption
    }
  }
}
