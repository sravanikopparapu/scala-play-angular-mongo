name := """ship-locator"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.scalaz" %% "scalaz-core" % "7.1.5",
  "org.webjars.bower" % "lodash" % "3.10.0",
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "angularjs" % "1.4.7",
  "org.webjars.bower" % "angular-bootstrap" % "0.14.3",
  "org.webjars.bower" % "ng-table" % "1.0.0-beta.5",
  specs2 % Test,
  json
)