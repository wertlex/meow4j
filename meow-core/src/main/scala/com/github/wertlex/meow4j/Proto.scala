package com.github.wertlex.meow4j

import

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

