import sbt._
import Keys._

object Deps {
  

  object TypesafeDependencies {
    lazy val deps   = Seq(play, json)
    lazy val json   = "com.typesafe.play" %% "play-json" % "2.3.7" 
    lazy val config = "com.typesafe" % "config" % "1.2.1" 
    lazy val play   = "com.typesafe.play" %% "play" % "2.3.7"
  }


  object ApacheLibs {
    object commons {
      lazy val io    = "commons-io" % "commons-io" % "2.4"
      lazy val lang  = "org.apache.commons" % "commons-lang3" % "3.1"
      lazy val codec = "commons-codec" % "commons-codec" % "1.10"
    }

    object http {
      lazy val client = "org.apache.httpcomponents" % "httpclient" % "4.3.1"
      lazy val mime   = "org.apache.httpcomponents" % "httpmime" % "4.3.1"
    }
  }

  object ScalaTime {
    lazy val scalaTime = "com.github.nscala-time" %% "nscala-time" % "1.6.0"
    lazy val deps = Seq(scalaTime)
  }

  object Scalaz {
    lazy val core = "org.scalaz" %% "scalaz-core" % "7.0.6"
    lazy val deps = Seq(core)
  }


  object Specs2 {
    lazy val specs2 = "org.specs2" %% "specs2-core" % "2.3.12" % "test"
    lazy val deps = Seq(specs2)
  }

  object Akka {
    lazy val actors     = "com.typesafe.akka" %% "akka-actor" % "2.3.10"
    lazy val slf4j      = "com.typesafe.akka" %% "akka-slf4j" % "2.3.10"
    lazy val http_core  = "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-RC2"
    lazy val http_dsl   = "com.typesafe.akka" % "akka-http-scala-experimental_2.11" % "1.0-RC2"
    lazy val coreDeps = Seq(actors, slf4j)
    lazy val httpDeps = Seq(http_core, http_dsl)
  }
  
  object Logback {
    lazy val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"
    lazy val deps = Seq(logback)
  }

  object Ficus {
    lazy val ficus = "net.ceedubs" %% "ficus" % "1.1.2"
    lazy val deps = Seq(ficus)
  }
  
  object Dispatch { 
    lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
    lazy val deps = Seq(dispatch)
  }
}
