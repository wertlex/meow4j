import com.github.wertlex.meow4j.{CypherStatement, CypherQuery, Database, DispatchNeoRestClient}
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent._
import com.github.wertlex.meow4j.NeoRestClient.Auth

val svc = url("http://api.hostip.info/country.php")
val country = Http(svc.OK(as.String))

Await.result(country, 10 seconds)


val q = url("http://localhost:7474/db/data/")
  .GET
  .addHeader("Authorization", "Basic bmVvNGo6MTExMTEx")

val q1 = q > { response =>
  "status: "  + response.getStatusCode +
  " headers: " + response.getHeaders +
  " body: "   + response.getResponseBody
}



//val result = Await.result(Http(q1), 10 seconds)

val textCypher =
  """
    |{
    |  "statements" : [ {
    |    "statement" : "CREATE (n) RETURN id(n)"
    |  } ]
    |}
  """.stripMargin

val client = new DispatchNeoRestClient("http://localhost:7474", false, Some(Auth("neo4j", "111111")))

Await.result(client.getServiceRoot, 10 seconds)

val jsObj = Json.parse(textCypher).as[JsObject]

Await.result(client.query(jsObj), 10 seconds)

Await.result(client.startTx(jsObj), 10 seconds)

val db = new Database(client)

val cypherQuery = CypherQuery("""CREATE (n {name: "Alex"}) RETURN n""")

val dualCypherQuery = CypherQuery.DefaultCypherQuery(List(
  CypherStatement("""CREATE (n {name: "Alex"}), (a) RETURN n,a """),
  CypherStatement("""CREATE (n {name: "John"}) RETURN n""")
))

Await.result(db.query(dualCypherQuery), 10 seconds)


