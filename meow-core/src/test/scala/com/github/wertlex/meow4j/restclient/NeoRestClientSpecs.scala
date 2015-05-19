package com.github.wertlex.meow4j.restclient

import com.github.wertlex.meow4j.restclient.NeoRestClient.Auth
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent._
import scalaz._
import Scalaz._

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
  def getNewClient()/*: NeoRestClient*/ = new DispatchNeoRestClient(uri, ssl, Option(neo4jAuth))
  /** Return client which not uses real db and poings somewhere. User for negative testing */
  def getBrokenClient(): NeoRestClient = new DispatchNeoRestClient("http://localhost:7070", false, None)

  /** Extracting id from transaction url */
  def getTxId(txUrl: String): String = {
    //"http://localhost:7474/db/data/transaction/7/commit"
    val arr = txUrl.split("""/""")
    arr(arr.length - 2)
  }

  def getTxId(response: NeoRestClient.Response): String = {
    getTxId((response.body \ "commit").as[String])
  }

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

  "DispatchNeoRestClient#query" should {
    "perform individual query" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )

      val r = Await.result( client.query(query), 10 seconds)
      r.status must beEqualTo(200)
      val expectedJson = Json.parse(
        """
          |{
          |  "results":[
          |    {
          |      "columns":["n"],
          |      "data":[{
          |        "row":[{"name":"Alex"}]
          |      }]
          |     }
          |   ],
          |  "errors":[]
          |} """.stripMargin)

      r.body must beEqualTo(expectedJson)
    }

    "perform multiple queries in one request" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a""")
        )
      )
      val r = Await.result(client.query(query), 10 seconds)
      r.status must beEqualTo(200)
      val expectedJson = Json.parse(
        """
          |{
          | "results": [
          |   {
          |    "columns":["n"],
          |    "data":[
          |      {"row":[{"name":"Alex"}]}
          |    ]
          |   },
          |   {
          |    "columns":["a"],
          |    "data":[
          |     {"row":[{"name":"Mike"}]}
          |    ]}
          | ],
          | "errors":[]
          |}""".stripMargin)
      r.body must beEqualTo(expectedJson)
    }

    "perform all queries before broken one, not perform after and contain an error" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a"""),
          Json.obj("statement" -> """CREATE (a {name: "John"}) RETURN a1"""), // a1 instead a intentionally to make an error
          Json.obj("statement" -> """CREATE (a {name: "Jack"}) RETURN a""")
        )
      )
      val r = Await.result(client.query(query), 10 seconds)
      r.status must beEqualTo(200)
      val expectedJsonResult = Json.parse(
        """
          |[
          |   {
          |    "columns":["n"],
          |    "data":[
          |      {"row":[{"name":"Alex"}]}
          |    ]
          |   },
          |   {
          |    "columns":["a"],
          |    "data":[
          |     {"row":[{"name":"Mike"}]}
          |    ]}
          |]
          |""".stripMargin)

      (r.body \ "results") must beEqualTo(expectedJsonResult)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
    }

    "return an error on single broken query" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """My Name is Bond. James Bond""")
        )
      )

      val r = Await.result( client.query(query), 10 seconds)
      r.status must beEqualTo(200)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
    }

    "return an error on bad json" in {
      val client = getNewClient()
      val query = Json.obj(
        "statementz"  -> Json.arr(
          Json.obj("z_tatement" -> """My Name is Bond. James Bond""")
        )
      )

      val r = Await.result( client.query(query), 10 seconds)
      r.status must beEqualTo(200)
      ((r.body \ "errors").as[JsArray].apply(0) \ "code").as[String] must beEqualTo("Neo.ClientError.Request.InvalidFormat")
    }

  }

  "DispatchNeoRestClient#startTx" should {
    "return transaction details in `commit` and `transaction` fields" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )
      val result = Await.result(client.startTx(query.some), 10 seconds)
      result.status must beEqualTo(201)
      (result.body \ "commit").asOpt[String] must beSome
      (result.body \ "results").asOpt[JsArray] must beSome
      (result.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

    "perform simple queries" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )

      val r = Await.result( client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(1)
    }

    "start tx without query payload" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr()
      )

      val r = Await.result( client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(0)
      (r.body \ "commit").asOpt[String] must beSome
    }

    "start tx by submitting empty json" in {
      val client = getNewClient()
      val query = Json.obj()

      val r = Await.result( client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(0)
      (r.body \ "commit").asOpt[String] must beSome
    }

    "start tx without any payload" in {
      val client = getNewClient()
      val query = Json.obj()

      val r = Await.result( client.startTx(None), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(0)
      (r.body \ "commit").asOpt[String] must beSome
    }

    "perform multiple queries in one request" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a""")
        )
      )
      val r = Await.result(client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(2)
    }

    "perform all queries before broken one, not perform after and contain an error" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a"""),
          Json.obj("statement" -> """CREATE (a {name: "John"}) RETURN a1"""), // a1 instead a intentionally to make an error
          Json.obj("statement" -> """CREATE (a {name: "Jack"}) RETURN a""")
        )
      )
      val r = Await.result(client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "results").as[JsArray].value.size must beEqualTo(2)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
    }

    "return an error on single broken query" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """My Name is Bond. James Bond""")
        )
      )

      val r = Await.result( client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
    }

    "return an error on bad json" in {
      val client = getNewClient()
      val query = Json.obj(
        "statementz"  -> Json.arr(
          Json.obj("z_tatement" -> """My Name is Bond. James Bond""")
        )
      )

      val r = Await.result( client.startTx(query.some), 10 seconds)
      r.status must beEqualTo(201)
      ((r.body \ "errors").as[JsArray].apply(0) \ "code").as[String] must beEqualTo("Neo.ClientError.Request.InvalidFormat")
    }
  }

  "DispatchNeoRestClient#queryInTx" should {
    "return error on non-existed transaction id" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )
      val r = Await.result(client.queryInTx("7", query), 10 seconds) // could be only an int
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
    }

    "fail on very wrong transaction id" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )
      val r = Await.ready(client.queryInTx("abc", query), 10 seconds) // could be only an int
      r.value.get must beFailedTry
    }


    "perform individual query" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n""")
        )
      )
      val txId = getTxId(Await.result(client.startTx(None), 10 seconds))
      val r = Await.result( client.queryInTx(txId, query), 10 seconds)
      r.status must beEqualTo(200)

      val expectedResultsJson = Json.parse(
        """
          |[
          |    {
          |      "columns":["n"],
          |      "data":[{
          |        "row":[{"name":"Alex"}]
          |      }]
          |     }
          |   ]
          |""".stripMargin)

      r.body \ "results" must beEqualTo(expectedResultsJson)
      (r.body \ "commit").asOpt[String] must beSome
      (r.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

    "perform multiple queries in one request" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a""")
        )
      )
      val txId = getTxId(Await.result(client.startTx(None), 10 seconds))
      val r = Await.result(client.queryInTx(txId, query), 10 seconds)
      r.status must beEqualTo(200)
      val expectedResultJson = Json.parse(
        """
          |[
          |   {
          |    "columns":["n"],
          |    "data":[
          |      {"row":[{"name":"Alex"}]}
          |    ]
          |   },
          |   {
          |    "columns":["a"],
          |    "data":[
          |     {"row":[{"name":"Mike"}]}
          |    ]}
          |]
          |""".stripMargin)
      r.body \ "results" must beEqualTo(expectedResultJson)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(0)
      (r.body \ "commit").asOpt[String] must beSome
      (r.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

    "perform all queries before broken one, not perform after and contain an error" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """CREATE (n {name: "Alex"}) RETURN n"""),
          Json.obj("statement" -> """CREATE (a {name: "Mike"}) RETURN a"""),
          Json.obj("statement" -> """CREATE (a {name: "John"}) RETURN a1"""), // a1 instead a intentionally to make an error
          Json.obj("statement" -> """CREATE (a {name: "Jack"}) RETURN a""")
        )
      )
      val txId = getTxId(Await.result(client.startTx(None), 10 seconds))
      val r = Await.result(client.queryInTx(txId, query), 10 seconds)
      r.status must beEqualTo(200)
      val expectedJsonResult = Json.parse(
        """
          |[
          |   {
          |    "columns":["n"],
          |    "data":[
          |      {"row":[{"name":"Alex"}]}
          |    ]
          |   },
          |   {
          |    "columns":["a"],
          |    "data":[
          |     {"row":[{"name":"Mike"}]}
          |    ]}
          |]
          |""".stripMargin)

      (r.body \ "results") must beEqualTo(expectedJsonResult)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
      (r.body \ "commit").asOpt[String] must beSome
      (r.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

    "return an error on single broken query" in {
      val client = getNewClient()
      val query = Json.obj(
        "statements"  -> Json.arr(
          Json.obj("statement" -> """My Name is Bond. James Bond""")
        )
      )

      val txId = getTxId(Await.result(client.startTx(None), 10 seconds))
      val r = Await.result( client.queryInTx(txId, query), 10 seconds)
      r.status must beEqualTo(200)
      (r.body \ "errors").as[JsArray].value.size must beEqualTo(1)
      (r.body \ "commit").asOpt[String] must beSome
      (r.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

    "return an error on bad json" in {
      val client = getNewClient()
      val query = Json.obj(
        "statementz"  -> Json.arr(
          Json.obj("z_tatement" -> """My Name is Bond. James Bond""")
        )
      )

      val txId = getTxId(Await.result(client.startTx(None), 10 seconds))
      val r = Await.result( client.queryInTx(txId, query), 10 seconds)
      r.status must beEqualTo(200)
      ((r.body \ "errors").as[JsArray].apply(0) \ "code").as[String] must beEqualTo("Neo.ClientError.Request.InvalidFormat")
      (r.body \ "commit").asOpt[String] must beSome
      (r.body \ "transaction" \ "expires").asOpt[String] must beSome
    }

  }

}


