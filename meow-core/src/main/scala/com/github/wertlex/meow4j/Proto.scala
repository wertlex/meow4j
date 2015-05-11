package com.github.wertlex.meow4j

import com.github.wertlex.meow4j.Models.Metadata
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._

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



trait RestClient {
  def ping: Future[Boolean]
  def getServiceRoot: Future[Metadata]
  def query(jsData: JsObject): Future[JsObject] // at least status code required
  def startTx(jsData: JsObject): Future[JsObject] // status code, transaction location
  def queryInTx(txId: String, jsData: JsObject): Future[JsObject] //
  def resetTimeoutTx(): Future[JsObject]
  def commitTx(jsData: JsObject): Future[JsObject]
  def rollbackTx(): Future[JsObject]
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