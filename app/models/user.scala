package models

case class Ship(name: String,
                width: Double, //in meters
                length: Double, //in meters
                draft: Double, //in meters
                lastSeen: Location
               )

case class Location(lon: Double, lat: Double)

object JsonFormats {

  import play.api.libs.json.Json

  implicit val locFormat = Json.format[Location]

  implicit val shipFormat = Json.format[Ship]
}
