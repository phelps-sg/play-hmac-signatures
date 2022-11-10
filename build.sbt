name := """play-hmac-signatures"""
organization := "sphelps.net"

version := "0.1-SNAPSHOT"

inThisBuild(
  List(
    scalaVersion := "2.13.10",
    scalafixScalaBinaryVersion := "2.13",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Ywarn-unused",
    scalacOptions += "-Xcheckinit"
  )
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.8.18",
  "com.google.inject" % "guice" % "5.1.0"
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.14"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
