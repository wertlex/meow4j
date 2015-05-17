package com.github.wertlex.meow4j.restclient

import dispatch._
import play.api.libs.json.JsObject

import scala.concurrent.ExecutionContext

/**
 * Neo Rest client interface.
 * Scala gateway for accessing basic goodies from Neo4j. Details on protocol can be found here:
 * http://neo4j.com/docs/milestone/rest-api.html
 *
 * TODO: where is ExecutionContext? As the only implementation for naw Dispatch-based, I should figure out where it
 * takes ExecutionContext
 *
 * User: wert
 * Date: 17.05.15
 * Time: 13:28
 */
trait NeoRestClient {
  def ping(implicit ec: ExecutionContext):                                      Future[Boolean]
  def getServiceRoot(implicit ec: ExecutionContext):                            Future[NeoRestClient.Response]
  def query(jsData: JsObject)(implicit ec: ExecutionContext):                   Future[NeoRestClient.Response]
  def startTx(jsData: JsObject)(implicit ec: ExecutionContext):                 Future[NeoRestClient.Response]
  def queryInTx(txId: String, jsData: JsObject)(implicit ec: ExecutionContext): Future[NeoRestClient.Response]
  def resetTimeoutTx(txId: String)(implicit ec: ExecutionContext):              Future[NeoRestClient.Response]
  def commitTx(txId: String, jsData: JsObject)(implicit ec: ExecutionContext):  Future[NeoRestClient.Response]
  def rollbackTx(txId: String)(implicit ec: ExecutionContext):                  Future[NeoRestClient.Response]
}


object NeoRestClient {
  case class Response(status: Int, body: JsObject)
  case class Auth(login: String, pass: String)
}
