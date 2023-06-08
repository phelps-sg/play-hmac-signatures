import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Dependencies extends AutoPlugin {

  object Version {
    val scala212 = "2.12.17"
    val scala213 = "2.13.11"

    val play = "2.8.19"
    val guice = "7.0.0"
    val jackson = "2.13.4"

    val scalatic = "3.2.16"
    val scalaMock = "5.2.0"
    val scalaTest = "3.2.16"
    val scalaTestPlus = "5.1.0"
  }

  override def trigger: PluginTrigger = allRequirements
  override def requires: sbt.Plugins = JvmPlugin

  import Version._

  override def projectSettings: Seq[
    Def.Setting[_ >: Seq[ModuleID] with Seq[Resolver] <: Seq[Serializable]]
  ] =
    Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % play,
        "com.google.inject" % "guice" % guice,
        "org.scalactic" %% "scalactic" % scalatic,
        "org.scalatest" %% "scalatest" % scalaTest % Test,
        "org.scalamock" %% "scalamock" % scalaMock % Test,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlus % Test
      )
    )
}
