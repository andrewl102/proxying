name := """proxying"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.rxtx" % "rxtx" % "2.1.7",
  "org.scream3r" % "jssc" % "2.8.0"
)
libraryDependencies += filters
libraryDependencies += "commons-io" % "commons-io" % "2.5"
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
