/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package higherkindness.mu.rpc.http

import cats.effect.{IO, _}
import higherkindness.mu.http.protocol.{HttpServer, RouteMap}
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze._
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global

class GreeterDerivedRestTests extends RpcBaseTestSuite with BeforeAndAfter {

  val host            = "localhost"
  val port            = 8080
  val serviceUri: Uri = Uri.unsafeFromString(s"http://$host:$port")

  implicit val ec                   = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]     = IO.timer(ec)

  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]

  val unaryRoute: RouteMap[IO] = UnaryGreeter.route[IO]

  val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, unaryRoute)

  var serverTask: Fiber[IO, Nothing] = _
  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
  after(serverTask.cancel.unsafeRunSync())

  "REST Service" should {

    val unaryClient = UnaryGreeter.httpClient[IO](serviceUri)

    // this is a new test, NOT REPLACING the currently removed tests
    "serve a GET request" in {
      val request  = Request[IO](Method.GET, serviceUri / "hello")
      val response = BlazeClientBuilder[IO](global).resource.use(_.expect[Json](request))
      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
    }

  }

}
