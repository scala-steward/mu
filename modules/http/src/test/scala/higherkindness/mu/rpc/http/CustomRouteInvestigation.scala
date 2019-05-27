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
import cats.effect._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.{Applicative, MonadError}
import higherkindness.mu.http.protocol._
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import higherkindness.mu.rpc.protocol._
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

@message final case class Ping(i: Int)

@message final case class Pong(i: Int)

@message final case class Another(s: String)

@service(Avro) trait CustomRouteInvestigation[F[_]] {

  // only including @http for the client. The server has been unrolled below
  @http def doPing(request: Ping): F[Pong]

  @http def getAnother(request: Empty.type): F[Another]

}

class CustomRouteInvestigationHandler[F[_]: Applicative](implicit F: MonadError[F, Throwable])
    extends CustomRouteInvestigation[F] {

  def doPing(ping: Ping): F[Pong] = Pong(ping.i + 1).pure

  def getAnother(request: Empty.type): F[Another] = Another("Yes").pure
}

/// BEGIN MACRO GENERATED
class CustomRouteInvestigationRestService[F[_]: Sync](
    doPingRoute: Option[PartialFunction[Request[F], F[Ping]]] = None,
    applyPongToResponse: Option[Kleisli[F, Pong, Response[F]]] = None,
    getAnotherRoute: Option[PartialFunction[Request[F], F[Empty.type]]] = None,
    applyAnotherToResponse: Option[Kleisli[F, Another, Response[F]]] = None
  )(implicit handler: CustomRouteInvestigationHandler[F],
    decoderPing: EntityDecoder[F, Ping],
    encoderPong: Encoder[Pong],
    encoderAnother: Encoder[Another])
    extends Http4sDsl[F] {

  // Ping/pong
  val doPingRouteDefault: PartialFunction[Request[F], F[Ping]] = {
    case msg @ POST -> Root / "doPing" => msg.as[Ping]
  }

  def applyPongToResponseDefault(
      implicit encoderPong: Encoder[Pong]): Kleisli[F, Pong, Response[F]] = Kleisli { pong =>
    Ok(pong.asJson)
  }

  //Another
  val getAnotherRouteDefault: PartialFunction[Request[F], F[Empty.type]] = {
    case msg @ GET -> Root / "getAnother" => Applicative[F].pure(Empty)
  }

  def applyAnotherToResponseDefault(
      implicit encoderAnother: Encoder[Another]): Kleisli[F, Another, Response[F]] = Kleisli { another =>
    Ok(another.asJson)
  }

  val routeForPingPong: PartialFunction[Request[F], F[Response[F]]] =
    doPingRoute.getOrElse(doPingRouteDefault)
      .andThen(_.flatMap(handler.doPing))
      .andThen(_.flatMap(applyPongToResponse.getOrElse(applyPongToResponseDefault).run))

  val routeForAnother: PartialFunction[Request[F], F[Response[F]]] =
    getAnotherRoute.getOrElse(getAnotherRouteDefault)
      .andThen(_.flatMap(handler.getAnother))
      .andThen(_.flatMap(applyAnotherToResponse.getOrElse(applyAnotherToResponseDefault).run))

  def service: HttpRoutes[F] = HttpRoutes.of[F] {
    routeForPingPong orElse routeForAnother
  }

}
/// END MACRO GENERATED

object CustomRoutesAndResponders extends Http4sDsl[IO] {

  // Ping/pong
  implicit def doPingRoute(
      implicit entityDecoderPing: EntityDecoder[IO, Ping]): PartialFunction[Request[IO], IO[Ping]] = {
    case msg @ POST -> Root / "doPingCustom" => msg.as[Ping]
  }

  implicit def applyPongToResponse(
      implicit encoderPong: Encoder[Pong]): Kleisli[IO, Pong, Response[IO]] = Kleisli { pong =>
    Ok(pong.asJson)
  }

  //Another
  implicit val getAnotherRoute: PartialFunction[Request[IO], IO[Empty.type]] = {
    case msg @ GET -> Root / "getAnother" => IO.pure(Empty)
  }

  implicit def applyAnotherToResponse(
      implicit encoderAnother: Encoder[Another]): Kleisli[IO, Another, Response[IO]] = Kleisli { another =>
    Ok(another.asJson)
  }
}

class CustomRouteInvestigationTest extends RpcBaseTestSuite {

  val host       = "localhost"
  val port       = 8080
  val serviceUri = Uri.unsafeFromString(s"http://$host:$port/")

  implicit val handlerIO: CustomRouteInvestigationHandler[IO] = new CustomRouteInvestigationHandler[IO]

  /// BEGIN MACRO GENERATED

  // this will be CustomRouteInvestigation.route[F]
  def routeCreator[F[_]: Sync](
      doPingRoute: Option[PartialFunction[Request[F], F[Ping]]] = None,
      applyPongToResponse: Option[Kleisli[F, Pong, Response[F]]] = None,
      getAnotherRoute: Option[PartialFunction[Request[F], F[Empty.type]]] = None,
      applyAnotherToResponse: Option[Kleisli[F, Another, Response[F]]] = None
    )(implicit handler: CustomRouteInvestigationHandler[F],
      decoderPing: _root_.org.http4s.EntityDecoder[F, Ping],
      encoderPong: _root_.io.circe.Encoder[Pong],
      encoderAnother: _root_.io.circe.Encoder[Another]
    ): _root_.higherkindness.mu.http.protocol.RouteMap[F] =
      _root_.higherkindness.mu.http.protocol
      .RouteMap[F]("CustomRouteInvestigation", new CustomRouteInvestigationRestService[F](
      doPingRoute, applyPongToResponse, getAnotherRoute, applyAnotherToResponse
    ).service)

  // END MACRO GENERATED

  import _root_.io.circe.generic.auto._
  implicit val entityDecoderPing: EntityDecoder[IO, Ping] = jsonOf[IO, Ping]

  val defaultRoutes: RouteMap[IO] = routeCreator[IO]()
  val  customRoutes: RouteMap[IO] = routeCreator[IO](doPingRoute = Some(CustomRoutesAndResponders.doPingRoute))

  val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]     = IO.timer(ec)

  "This test" should {

    import _root_.io.circe.generic.auto._

    val client = CustomRouteInvestigation.httpClient[IO](serviceUri)

    "work for doPing" in {
      val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, defaultRoutes)
      val serverTask: Fiber[IO, Nothing] = server.resource.use(_ => IO.never).start.unsafeRunSync()

      val response: IO[Pong] = BlazeClientBuilder[IO](ec).resource.use(client.doPing(Ping(1)))

      response.unsafeRunSync() shouldBe Pong(2)

      serverTask.cancel.unsafeRunSync()
    }

    "work for doPing with custom route and client" in {
      val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, customRoutes)
      val serverTask: Fiber[IO, Nothing] = server.resource.use(_ => IO.never).start.unsafeRunSync()

      val request  = Request[IO](Method.POST, serviceUri / "CustomRouteInvestigation" / "doPingCustom")
      val response = BlazeClientBuilder[IO](ec).resource.use(_.expect[Json](request.withEntity(Ping(1).asJson)))

      response.unsafeRunSync() shouldBe Pong(2).asJson

      serverTask.cancel.unsafeRunSync()
    }

    "work for getAnother" in {
      val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, defaultRoutes)
      val serverTask: Fiber[IO, Nothing] = server.resource.use(_ => IO.never).start.unsafeRunSync()

      val response: IO[Another] = BlazeClientBuilder[IO](ec).resource.use(client.getAnother(_))

      response.unsafeRunSync() shouldBe Another("Yes")

      serverTask.cancel.unsafeRunSync()
    }

  }
}
