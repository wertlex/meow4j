package com.github.wertlex.meow4j

import com.github.wertlex.meow4j.Models.{ErrorData, DefaultResponse, Metadata}

import dispatch._
import dispatch.Defaults._
import play.api.libs.json._
import org.apache.commons.codec.binary.Base64

import scala.util.control.NonFatal
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import com.github.wertlex.meow4j.restclient._

/**
 * User: wert
 * Date: 11.05.15
 * Time: 20:28
 */
class Database(client: NeoRestClient) {

  import Models._

  def query[QUERY <% CypherQuery](cypher: QUERY) = {
    client.query(cypherQueryWrites.writes(cypher)).map{ response =>
      response.body.as[DefaultResponse]
    }

  }

  private implicit val cypherStatementWrites: OWrites[CypherStatement] = new OWrites[CypherStatement] {
    override def writes(o: CypherStatement) = Json.obj(
      "statement" -> o.statement
//      "parameters"  ->  // TODO: implement me
    )
  }

  private implicit val cypherQueryWrites: OWrites[CypherQuery] = new OWrites[CypherQuery] {
    override def writes(o: CypherQuery): JsObject = Json.obj(
      "statements" -> o.statements
    )
  }
}

object Database {

  def connect(uri: String, ssl: Boolean): Option[Database] = ???

}


class QueryBuilder(futureResponse: => Future[DefaultResponse]) {
  def raw: Future[List[ErrorData] Either List[List[JsValue]]] = {
    futureResponse.map { response =>
      response.results.flatMap(_.data.map(_.row))
    }
    ???
  }

  def as = ???
  def run = ???
}

trait CypherQuery {
  def statements: List[CypherStatement]
}

object CypherQuery {
  case class DefaultCypherQuery(statements: List[CypherStatement]) extends CypherQuery

  def apply(statement: String): CypherQuery = DefaultCypherQuery(List(CypherStatement(statement)))
}


trait CypherStatement {
  def statement: String
  def parameters: Map[String, Map[String, Any]]
}

object CypherStatement {
  case class DefaultCypherStatement(statement: String, parameters: Map[String, Map[String, Any]]) extends CypherStatement

  def apply(statement: String): CypherStatement = DefaultCypherStatement(statement, Map())
  def apply(statement: String, parameters: Map[String, Map[String, Any]]): CypherStatement = DefaultCypherStatement(
    statement, parameters
  )
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

  trait Response {
    def results:  List[ResultData]
    def errors:   List[ErrorData]
  }



  case class DefaultResponse(
    results:  List[ResultData],
    errors:   List[ErrorData]
  ) extends Response {
  }

  case class TxResponse(
    results:      List[ResultData],
    errors:       List[ErrorData],
    commit:       String,
    transaction:  TransactionData
  ) extends Response

  case class ResultData(columns: List[String], data: List[RowData])
  case class RowData(row: List[JsValue])
  case class ErrorData(code: String, message: String)
  case class TransactionData(expires: String)



  implicit val errorDataReads: Reads[ErrorData] = (
    (__ \ "code").read[String] ~
    (__ \ "message").read[String]
  )(ErrorData.apply _)

  implicit val transactionDataReads: Reads[TransactionData] = new Reads[TransactionData] {
    override def reads(json: JsValue): JsResult[TransactionData] =
      (json \ "expires").validate[String].map(TransactionData(_))
  }

  implicit val rowDataReads: Reads[RowData] = new Reads[RowData] {
    override def reads(json: JsValue): JsResult[RowData] = {
      (json \ "row").validate[JsArray].map(jsArr => RowData(jsArr.value.toList))
    }
  }

  implicit val resultDataReads: Reads[ResultData] = (
    (__ \ "columns").read[List[String]] ~
    (__ \ "data").read[List[RowData]]
  )(ResultData.apply _)

  implicit val defaultResponseReads: Reads[DefaultResponse] = (
    (__ \ "results").read[List[ResultData]] ~
      (__ \ "errors").read[List[ErrorData]]
    )(DefaultResponse)

  implicit val txResponseReads: Reads[TxResponse] = (
    (__ \ "results").read[List[ResultData]] ~
      (__ \ "errors").read[List[ErrorData]] ~
      (__ \ "commit").read[String] ~
      (__ \ "transaction").read[TransactionData]
    )(TxResponse)
}

/** Maybe someday it will be ported to scala parser combinators. But not today */
object Parsers {

  import scalaz._
  import Scalaz._

  trait RowParser[A] {
    def parse(row: List[JsValue]): Option[A]
  }

  /** Return row as is */
  object AsIsRowParser extends RowParser[List[JsValue]] {
    def parse(row: List[JsValue]): Option[List[JsValue]] = Option(row)
  }

  /** Parse row into String. Not really a parser, but converter */
  object StringRowParser extends RowParser[String] {
    def parse(row: List[JsValue]): Option[String] = Option(row.toString())
  }

  class FirstItemRowParser[A](reads: Reads[A]) extends RowParser[A] {
    def parse(row: List[JsValue]): Option[A] = row.headOption.flatMap(js => js.asOpt[A](reads))
  }

  class ListRowParser[A](reads: Reads[A]) extends RowParser[List[A]] {
    def parse(row: List[JsValue]): Option[List[A]] = row.map(js => js.asOpt[A](reads)).sequence
  }

  class Tuple2RowParser[A, B](aReads: Reads[A], bReads: Reads[B]) extends RowParser[(A, B)] {
    def parse(row: List[JsValue]): Option[(A, B)] =
      for {
        verifiedRow <- Option(row).filter(_.size >= 2)
        vRow        = verifiedRow.toVector
        a           <- vRow(0).asOpt[A](aReads)
        b           <- vRow(1).asOpt[B](bReads)
      } yield (a, b)
  }

  class Tuple3RowParser[A, B, C](aReads: Reads[A], bReads: Reads[B], cReads: Reads[C]) extends RowParser[(A, B, C)] {
    def parse(row: List[JsValue]): Option[(A, B, C)] =
      for {
        verifiedRow <- Option(row).filter(_.size >= 3)
        vRow        = verifiedRow.toVector
        a           <- vRow(0).asOpt[A](aReads)
        b           <- vRow(1).asOpt[B](bReads)
        c           <- vRow(2).asOpt[C](cReads)
      } yield (a, b, c)
  }
}