name := """play-hmac-signatures"""
version := "0.2.0"
organization := "com.mesonomics"
homepage := Some(url("https://github.com/phelps-sg/play-hmac-signatures"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/phelps-sg/play-hmac-signatures"),
    "git@github.com:phelps-sg/play-hmac-signatures.git"
  )
)
developers := List(
  Developer(
    "phelps-sg",
    "Steve Phelps",
    "sphelps@sphelps.net",
    url("https://github.com/usernamehttps://github.com/phelps-sg")
  )
)
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle := true

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

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
