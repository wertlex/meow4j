package com.github.wertlex.meow4j.restclient

import dispatch._
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{Json, JsObject}
import com.ning.http.client.{Response => NingResponse}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
 * Check docuumentation on protocol implementation here:
 * http://neo4j.com/docs/milestone/rest-api.html
 * User: wert
 * Date: 17.05.15
 * Time: 13:35
 */
class DispatchNeoRestClient(uri: String, ssl: Boolean, auth: Option[NeoRestClient.Auth]) extends NeoRestClient {

  def ping(implicit ec: ExecutionContext): Future[Boolean] = {
    getServiceRoot.map(_ => true).recover { case NonFatal(e) => false }
  }

  def getServiceRoot(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/")
      .GET
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addOptHeader("Authorization", optionalAuthHeaderValue)

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def query(jsData: JsObject)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/commit")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }


  def startTx(jsData: JsObject)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def queryInTx(txId: String, jsData: JsObject)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/$txId")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def resetTimeoutTx(txId: String)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    queryInTx(txId, Json.parse("""{"statements" : [ ]}""").as[JsObject])
  }

  def commitTx(txId: String, jsData: JsObject)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
    val query = url(s"$uri/db/data/transaction/$txId/commit")
      .POST
      .addHeader("Accept", "application/json; charset=UTF-8")
      .addHeader("Content-Type", "application/json")
      .addOptHeader("Authorization", optionalAuthHeaderValue) << jsData.toString()

    val pair = query > responseToNeoResponse _

    Http(pair)
  }

  def rollbackTx(txId: String)(implicit ec: ExecutionContext): Future[NeoRestClient.Response] = {
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

