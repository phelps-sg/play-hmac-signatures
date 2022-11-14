/*
 * Copyright (C) 2021 Just Play LTDA.
 * See the LICENCE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 */

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Dependencies extends AutoPlugin {

  object Version {
    val play = "2.8.18"
    val guice = "5.1.0"
    val scalatic = "3.2.14"
    val scalaTest = "3.2.14"
    val scalaTestPlus = "5.1.0"
    val jackson = "2.13.4"
  }

  override def trigger: PluginTrigger = allRequirements
  override def requires: sbt.Plugins = JvmPlugin

  import Version._

  override def projectSettings: Seq[
    Def.Setting[_ >: Seq[ModuleID] with Seq[Resolver] <: Seq[Serializable]]
  ] =
    Seq(
      dependencyOverrides ++= Seq(
        "com.fasterxml.jackson.core" % "jackson-databind" % jackson
      ),
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % play,
        "com.google.inject" % "guice" % guice,
        "org.scalactic" %% "scalactic" % scalatic,
        "org.scalatest" %% "scalatest" % scalaTest % Test,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlus % Test
      )
    )
}
