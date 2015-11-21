package models

import java.io.InputStream

import play.api.libs.json.{DefaultReads, JsArray, JsObject, _}

import scala.util.Try

case class Ship(name: String,
                width: Double, //in meters
                length: Double, //in meters
                draft: Double, //in meters
                loc: Point
               )

case class Point(lon: Double, lat: Double)

object JsonFormats extends DefaultReads {

  import play.api.libs.json.Json

  implicit object PointWriter extends Writes[Point] {
    def writes(point: Point): JsValue = Json.obj(
      "type" -> "Point",
      "coordinates" -> Seq(point.lat, point.lon))
  }

  implicit object PointReader extends Reads[Point] {
    def reads(json: JsValue): JsResult[Point] = {
      val coordinates = (json \ "coordinates").as[List[Double]]
      val lon = coordinates(0)
      val lat = coordinates(1)
      JsSuccess(Point(lon, lat))
    }
  }

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
          loc = Point(
            lon = v("LON").as[String].toDouble,
            lat = v("LAT").as[String].toDouble
          )
        )
      }.toOption
    }
  }
}
