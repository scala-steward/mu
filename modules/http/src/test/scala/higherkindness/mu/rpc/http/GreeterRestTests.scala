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

//class GreeterRestTests extends RpcBaseTestSuite with BeforeAndAfter {
//
//  val Hostname = "localhost"
//  val Port     = 8080
//
//  val serviceUri: Uri = Uri.unsafeFromString(s"http://$Hostname:$Port")
//
//  implicit val cs: ContextShift[IO] = IO.contextShift(global)
//  implicit val timer: Timer[IO]     = IO.timer(global)
//
//  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]
//
//  val unaryService: HttpRoutes[IO] = new UnaryGreeterRestService[IO].service
//
//  val server: BlazeServerBuilder[IO] = BlazeServerBuilder[IO]
//    .bindHttp(Port, Hostname)
//    .withHttpApp(Router(s"/" -> unaryService).orNotFound)
//
//  var serverTask: Fiber[IO, Nothing] = _
//  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
//  after(serverTask.cancel.unsafeRunSync)
//
//  "REST Server" should {
//
//    "serve a GET request" in {
//      val request  = Request[IO](Method.GET, serviceUri)
//      val response = BlazeClientBuilder[IO](global).resource.use(_.expect[Json](request))
//      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
//    }
//
//  }
//}
