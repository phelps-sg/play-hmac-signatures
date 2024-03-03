import Common._
import Dependencies.Version.scala213

name := """play-hmac-signatures"""

enablePlugins(Common, Dependencies)

inThisBuild(
  List(
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213),
    scalafixScalaBinaryVersion := "2.13",
    versionScheme := Some("early-semver"),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Ywarn-unused",
    scalacOptions += "-Xcheckinit",
    sonatypePublishTo := Some({
      if (isSnapshot.value)
        Opts.resolver.sonatypeOssSnapshots.head
      else
        Opts.resolver.sonatypeStaging
    }),
    scmInfo := Some(
      ScmInfo(
        url(s"https://github.com/$gitAccount/$repoName"),
        s"git@github.com:$gitAccount/$repoName.git"
      )
    )
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
