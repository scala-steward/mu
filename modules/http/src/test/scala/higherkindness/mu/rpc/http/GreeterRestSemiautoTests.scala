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

import cats.data.Kleisli
import cats.effect.{IO, _}
import fs2.Stream
import higherkindness.mu.http.{ResponseError, UnexpectedError}
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.protocol.Empty
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.Method._
import org.scalatest._
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import io.circe.syntax._
import higherkindness.mu.http.implicits._
import higherkindness.mu.http.protocol.{HttpServer, RouteMap}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class GreeterRestSemiautoTests extends RpcBaseTestSuite with BeforeAndAfter {

  val Hostname = "localhost"
  val Port     = 8080

  val serviceUri: Uri = Uri.unsafeFromString(s"http://$Hostname:$Port")

  val UnaryServicePrefix = "UnaryGreeter"
  val Fs2ServicePrefix   = "Fs2Greeter"
  val MonixServicePrefix = "MonixGreeter"

  implicit val ec                   = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]     = IO.timer(ec)

  implicit val unaryHandlerIO = new UnaryGreeterHandler[IO]
//  implicit val fs2HandlerIO   = new Fs2GreeterHandler[IO]

  //Derivation
  class UnaryGreeterDI[F[_]: Sync](prefix: Option[String] = None)(
      implicit handler: UnaryGreeter[F],
      decoderHelloRequest: io.circe.Decoder[HelloRequest],
      encoderHelloResponse: io.circe.Encoder[HelloResponse])
      extends Http4sDsl[F] {

    val p = prefix.getOrElse("UnaryGreeter")

    private implicit val requestDecoder: EntityDecoder[F, HelloRequest] = jsonOf[F, HelloRequest]

    val requestEmptyPF: PartialFunction[Request[F], F[Empty.type]] = {
      case GET -> Root / "getHello" => Empty.pure
    }

    val requestHelloRequestPF: PartialFunction[Request[F], F[HelloRequest]] = {
      case msg @ POST -> Root / "sayHello" => msg.as[HelloRequest]
    }

    val responseHelloResponseKleisli: Kleisli[F, HelloResponse, Response[F]] = Kleisli {
      case r @ HelloResponse(_) => Ok(r.asJson).adaptErrors
    }

    val getHello: PartialFunction[Request[F], F[Response[F]]] =
      requestEmptyPF
        .andThen(_.flatMap(handler.getHello))
        .andThen(_.flatMap(responseHelloResponseKleisli.run))

    val sayHello: PartialFunction[Request[F], F[Response[F]]] =
      requestHelloRequestPF
        .andThen(_.flatMap(handler.sayHello))
        .andThen(_.flatMap(responseHelloResponseKleisli.run))

    def service: RouteMap[F] =
      RouteMap[F](p, HttpRoutes.of(getHello orElse sayHello))
  }

  class HttpClientUnaryGreeter[F[_]: Sync](uri: Uri) {

    def getHello(client: Client[F])(
        implicit responseDecoder: Decoder[HelloResponse]): F[HelloResponse] = {
      implicit val responseEntityDecoder: EntityDecoder[F, HelloResponse] = jsonOf[F, HelloResponse]
      val request                                                         = Request[F](GET, uri / "getHello")
      client.expectOr[HelloResponse](request)(handleResponseError)(jsonOf[F, HelloResponse])
    }

    def sayHello(req: HelloRequest)(client: Client[F])(
        implicit requestEncoder: Encoder[HelloRequest],
        responseDecoder: Decoder[HelloResponse]): F[HelloResponse] = {
      implicit val responseEntityDecoder: _root_.org.http4s.EntityDecoder[F, HelloResponse] =
        jsonOf[F, HelloResponse]
      val request = Request[F](POST, uri / "sayHello").withEntity(req.asJson)
      client.expectOr[HelloResponse](request)(handleResponseError)(jsonOf[F, HelloResponse])
    }
  }

  def httpClientUnaryGreeter[F[_]: Sync](uri: Uri) =
    new HttpClientUnaryGreeter[F](uri / "UnaryGreeter")

  //User

//  class Stuff[F[_]: Sync] extends Http4sDsl[F] {
//
//
//
//  }

  val unaryServiceDI             = new UnaryGreeterDI[IO]
  val unaryService: RouteMap[IO] = unaryServiceDI.service

  val server: BlazeServerBuilder[IO] = HttpServer.bind(Port, Hostname, unaryService)

  var serverTask: Fiber[IO, Nothing] = _
  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
  after(serverTask.cancel)

  val unaryClient = httpClientUnaryGreeter[IO](serviceUri)

  "REST Server" should {

    "serve a GET request" in {
//      val request: Request[IO] = Request[IO](Method.GET, serviceUri / unaryServiceDI.p / "getHello")
//      val h = _.expect[Json](request)
      val response = BlazeClientBuilder[IO](ec).resource.use(unaryClient.getHello(_))

      response.unsafeRunSync() shouldBe HelloResponse("hey")
    }

//    "serve a POST request" in {
//      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
//      val requestBody = HelloRequest("hey").asJson
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
//      response.unsafeRunSync() shouldBe HelloResponse("hey").asJson
//    }
//
//    "return a 400 Bad Request for a malformed unary POST request" in {
//      val request     = Request[IO](Method.POST, serviceUri / UnaryServicePrefix / "sayHello")
//      val requestBody = "{"
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
//      the[UnexpectedStatus] thrownBy response.unsafeRunSync() shouldBe UnexpectedStatus(
//        Status.BadRequest)
//    }
//
//    "return a 400 Bad Request for a malformed streaming POST request" in {
//      val request     = Request[IO](Method.POST, serviceUri / Fs2ServicePrefix / "sayHellos")
//      val requestBody = "{"
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(requestBody)))
//      the[UnexpectedStatus] thrownBy response.unsafeRunSync() shouldBe UnexpectedStatus(
//        Status.BadRequest)
//    }

  }

//  val unaryServiceClient: UnaryGreeterRestClient[IO] =
//    new UnaryGreeterRestClient[IO](serviceUri / UnaryServicePrefix)
//  val fs2ServiceClient: Fs2GreeterRestClient[IO] =
//    new Fs2GreeterRestClient[IO](serviceUri / Fs2ServicePrefix)
//
//  "REST Service" should {
//
//    "serve a GET request" in {
//      val response = BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.getHello()(_))
//      response.unsafeRunSync() shouldBe HelloResponse("hey")
//    }
//
//    "serve a unary POST request" in {
//      val request = HelloRequest("hey")
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
//      response.unsafeRunSync() shouldBe HelloResponse("hey")
//    }
//
//    "handle a raised gRPC exception in a unary POST request" in {
//      val request = HelloRequest("SRE")
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
//      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
//        Status.BadRequest,
//        Some("INVALID_ARGUMENT: SRE"))
//    }
//
//    "handle a raised non-gRPC exception in a unary POST request" in {
//      val request = HelloRequest("RTE")
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
//      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
//        Status.InternalServerError,
//        Some("RTE"))
//    }
//
//    "handle a thrown exception in a unary POST request" in {
//      val request = HelloRequest("TR")
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(unaryServiceClient.sayHello(request)(_))
//      the[ResponseError] thrownBy response.unsafeRunSync() shouldBe ResponseError(
//        Status.InternalServerError)
//    }
//
//    "serve a POST request with fs2 streaming request" in {
//      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
//      response.unsafeRunSync() shouldBe HelloResponse("hey, there")
//    }
//
//    "serve a POST request with empty fs2 streaming request" in {
//      val requests = Stream.empty
//      val response =
//        BlazeClientBuilder[IO](ec).resource.use(fs2ServiceClient.sayHellos(requests)(_))
//      response.unsafeRunSync() shouldBe HelloResponse("")
//    }
//
//    "serve a POST request with fs2 streaming response" in {
//      val request = HelloRequest("hey")
//      val responses =
//        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
//      responses.compile.toList
//        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("hey"))
//    }
//
//    "handle errors with fs2 streaming response" in {
//      val request = HelloRequest("")
//      val responses =
//        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHelloAll(request)(_))
//      the[UnexpectedError] thrownBy responses.compile.toList
//        .unsafeRunSync() should have message "java.lang.IllegalArgumentException: empty greeting"
//    }
//
//    "serve a POST request with bidirectional fs2 streaming" in {
//      val requests = Stream(HelloRequest("hey"), HelloRequest("there"))
//      val responses =
//        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
//      responses.compile.toList
//        .unsafeRunSync() shouldBe List(HelloResponse("hey"), HelloResponse("there"))
//    }
//
//    "serve an empty POST request with bidirectional fs2 streaming" in {
//      val requests = Stream.empty
//      val responses =
//        BlazeClientBuilder[IO](ec).stream.flatMap(fs2ServiceClient.sayHellosAll(requests)(_))
//      responses.compile.toList.unsafeRunSync() shouldBe Nil
//    }
//
//  }
//
}
