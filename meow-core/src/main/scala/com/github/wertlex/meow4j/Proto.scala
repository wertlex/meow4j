package com.github.wertlex.meow4j

import com.github.wertlex.meow4j.Models.Metadata
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._
import org.apache.commons.codec.binary.Base64

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
  def getServiceRoot: Future[JsObject]
  def query(jsData: JsObject): Future[JsObject] // at least status code required
  def startTx(jsData: JsObject): Future[JsObject] // status code, transaction location
  def queryInTx(txId: String, jsData: JsObject): Future[JsObject] //
  def resetTimeoutTx(): Future[JsObject]
  def commitTx(jsData: JsObject): Future[JsObject]
  def rollbackTx(): Future[JsObject]
}

object NeoRestClient {
  case class Response(status: Int, body: JsObject)
}

class DispatchNeoRestClient(uri: String, ssl: Boolean, login: String, pass: String) /*extends NeoRestClient*/ {
  def getServiceRoot: Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/")
      .GET
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Authorization", authorizationHeaderValue)

    val pair = query > { response =>
      val status  = response.getStatusCode
      val body    = response.getResponseBody
      val js      = Json.parse(body).as[JsObject]
      NeoRestClient.Response(status, js)
    }

    Http(pair)
  }

//  def query(jsData: JsObject): Future[JsObject] = {
//    val query = url(s"$uri")
//  }

  private val authorizationHeaderValue: String = new String(Base64.encodeBase64(s"$login:$pass".getBytes))

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