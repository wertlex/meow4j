package com.github.wertlex.meow4j.restclient

import com.github.wertlex.meow4j.restclient.NeoRestClient.Auth
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import scala.concurrent._

/**
 * User: wert
 * Date: 17.05.15
 * Time: 14:32
 */
class DispatchNeoRestClientSpecs extends Specification with NoTimeConversions{

  sequential

  import scala.concurrent.ExecutionContext.Implicits.global

  val uri: String = "http://localhost:7474"
  val ssl: Boolean = false
  val neo4jAuth: Auth = new Auth("neo4j", "111111")

  /** Return client which uses real up and running db */
  def getNewClient(): NeoRestClient = new DispatchNeoRestClient(uri, ssl, Option(neo4jAuth))
  /** Return client which not uses real db and poings somewhere. User for negative testing */
  def getBrokenClient(): NeoRestClient = new DispatchNeoRestClient("http://localhost:7070", false, None)

  "DispatchNeoRestClient#ping" should {
    "return true when db presented" in {
      val client = getNewClient()
      client.ping must beTrue.await(1, 10 seconds)
    }

    "return false when no db presented" in {
      val client = getBrokenClient()
      client.ping must beFalse.await(1, 10 seconds)
    }
  }

  "DispatchNeoRestClient#getServiceRoot" should {
    "return a lot of json with meta info about database instance" in {
      val client = getNewClient()
      val response = Await.result(client.getServiceRoot, 10 seconds)
      response.status must beEqualTo(200)
      (response.body \ "neo4j_version").asOpt[String] must beSome
      (response.body \ "node").asOpt[String] must beSome
    }

    "return failed future when there is no such database" in {
      val client = getBrokenClient()
      // waiting for value and converting it to Try
      val r = Await.ready(client.getServiceRoot, 10 seconds)
      r.value must beSome
      r.value.get must beFailedTry
    }
  }

//  "DispatchNeoRestClient#query" should {
//    "perform individual query" in {
//      val client = getNewClient()
//
//    }
//  }

}


