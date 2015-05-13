package com.github.wertlex.meow4j

import com.github.wertlex.meow4j.Models.Metadata
import com.ning.http.client.{Response => NingResponse}
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._
import org.apache.commons.codec.binary.Base64

import scala.util.control.NonFatal

/**
 * User: wert
 * Date: 11.05.15
 * Time: 20:28
 */
class Database {

  def query[QUERY <% CypherQuery](cypher: QUERY): QueryBuilder = ???
}

object Database {

  def connect(uri: String, ssl: Boolean): Option[Database] = ???

}




class CypherQuery
class QueryBuilder



trait NeoRestClient {
  def ping: Future[Boolean]
  def getServiceRoot: Future[NeoRestClient.Response]
  def query(jsData: JsObject): Future[NeoRestClient.Response]
  def startTx(jsData: JsObject): Future[NeoRestClient.Response]
  def queryInTx(txId: String, jsData: JsObject): Future[NeoRestClient.Response]
  def resetTimeoutTx(txId: String): Future[NeoRestClient.Response]
  def commitTx(txId: String, jsData: JsObject): Future[NeoRestClient.Response]
  def rollbackTx(txId: String): Future[NeoRestClient.Response]
}

object NeoRestClient {
  case class Response(status: Int, body: JsObject)
  case class Auth(login: String, pass: String)
}

class DispatchNeoRestClient(uri: String, ssl: Boolean, auth: Option[NeoRestClient.Auth]) extends NeoRestClient {

  def ping: Future[Boolean] = {
    getServiceRoot.map(_ => true).recover { case NonFatal(e) => false}
  }

  def getServiceRoot: Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/")
      .GET
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addOptHeader("Authorization", optionalAuthHeaderValue)


    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def query(jsData: JsObject): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/commit")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }



  def startTx(jsData: JsObject): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def queryInTx(txId: String, jsData: JsObject): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/$txId")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def resetTimeoutTx(txId: String): Future[NeoRestClient.Response] = {
    queryInTx(txId, Json.parse("""{"statements" : [ ]}""").as[JsObject])
  }

  def commitTx(txId: String, jsData: JsObject): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/$txId/commit")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def rollbackTx(txId: String): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/$txId/commit")
      .DELETE
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addOptHeader("Authorization", optionalAuthHeaderValue)

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  /** Converts response from neo4j rest server to understandable form */
  private def responseToNeoResponse(response: NingResponse): NeoRestClient.Response = {
    val status  = response.getStatusCode
    val body    = response.getResponseBody
    val js      = Json.parse(body).as[JsObject]
    NeoRestClient.Response(status, js)
  }

  /** Calculate authorization header value based on auth */
  private val optionalAuthHeaderValue: Option[String] = auth.map { a =>
    new String(Base64.encodeBase64(s"${a.login}:${a.pass}".getBytes))
  }

  /** Add .addOptHeader() method to Req */
  private implicit class ReqWithOptionalHeader(req: Req) {
    def addOptHeader(headerName: String, optHeaderValue: Option[String]): Req = optHeaderValue match {
      case Some(v)  => req.addHeader(headerName, v)
      case None     => req
    }

  }
}

object Models {

  /** Neo4j instance metadata.
    * Check details here: http://neo4j.com/docs/milestone/rest-api-service-root.html
    */
  case class Metadata(
    extensions:         List[String],
    node:               String,
    node_index:         String,
    relationship_index: String,
    extensions_info:    String,
    relationship_types: String,
    batch:              String,
    cypher:             String,
    indexes:            String,
    constraints:        String,
    transaction:        String,
    node_labels:        String,
    neo4j_version:      String
  )

}