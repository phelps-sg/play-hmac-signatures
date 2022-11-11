name := """play-hmac-signatures"""
organization := "sphelps.net"

version := "0.1-SNAPSHOT"

enablePlugins(Dependencies)

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

// Fixing Scala-Xml mess (Temp).
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

addCommandAlias(
  "validateCode",
  List(
    "scalafix",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "test:scalafix",
    "test:scalafmtCheckAll"
  ).mkString(";")
)

addCommandAlias(
  "formatCode",
  List(
    "scalafmt",
    "scalafmtSbt",
    "Test/scalafmt"
  ).mkString(";")
)

addCommandAlias(
  "testWithCoverage",
  List(
    "coverage",
    "test",
    "coverageReport"
  ).mkString(";")
)
