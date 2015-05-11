import sbt._
import Keys._
import Deps._


object ApplicationBuild extends Build {
  val appName         = "MEOW4J"
  val appVersion      = "develop"

  lazy val standardSettings = Defaults.defaultSettings ++ Seq(
    organization := "WertLex",
    version := appVersion,
    scalaVersion := "2.11.6",
    exportJars := true,
    resolvers += "Online Play Repository" at "http://repo.typesafe.com/typesafe/simple/maven-releases/",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:postfixOps", "-language:implicitConversions")
  )

  lazy val coreDeps = Seq(
  ) ++ Akka.coreDeps ++ Akka.httpDeps ++ Scalaz.deps ++ Specs2.deps ++ Ficus.deps

  lazy val core = Project(
    id   = "MEOW-Core",
    base = file("meow-core"),
    settings = standardSettings ++ Seq(libraryDependencies ++= coreDeps)
  )  

  override def rootProject = Some(core)

}
