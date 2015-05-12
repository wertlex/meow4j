import com.github.wertlex.meow4j.DispatchNeoRestClient
import dispatch._
import dispatch.Defaults._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent._

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

val client = new DispatchNeoRestClient("http://localhost:7474", false, "neo4j", "111111")


Await.result(client.getServiceRoot, 10 seconds)




