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

import cats.{Applicative, MonadError}
import cats.effect.{IO, _}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import higherkindness.mu.rpc.protocol._
import higherkindness.mu.http.protocol._
import higherkindness.mu.rpc.common.RpcBaseTestSuite
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityDecoder, HttpRoutes, Uri}
import org.scalatest._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

@message final case class Ping(i: Int)

@message final case class Pong(i: Int)

@service(Avro) trait ImplicitInvestigation[F[_]] {

  // only including @http for the client. The server has been unrolled below
  @http def doPing(request: Ping): F[Pong]

}

class ImplicitInvestigationHandler[F[_]: Applicative](implicit F: MonadError[F, Throwable])
    extends ImplicitInvestigation[F] {

  def doPing(ping: Ping): F[Pong] = Pong(ping.i + 1).pure
}

class ImplicitInvestigationRestService[F[_]](
    implicit handler: ImplicitInvestigationHandler[F],
    decoderPing: Decoder[Ping],
    encoderPong: Encoder[Pong],
    F_better_name_please: Sync[F])
    extends Http4sDsl[F] {

  implicit private val entityDecoderPing: EntityDecoder[F, Ping] = jsonOf[F, Ping]

  def service = HttpRoutes.of[F] {
    case msg @ POST -> Root / "doPing" =>
      msg.as[Ping].flatMap(request => Ok(handler.doPing(request).map(_.asJson)))

  }

}

class ImplicitInvestigationTest extends RpcBaseTestSuite with BeforeAndAfter {

  val host       = "localhost"
  val port       = 8080
  val serviceUri = Uri.unsafeFromString(s"http://$host:$port/")

  implicit val handlerIO: ImplicitInvestigationHandler[IO] = new ImplicitInvestigationHandler[IO]

  /// BEGIN MACRO GENERATED

  // this will be ImplicitInvestigation.route[F]
  def routeCreator[F[_]](
      implicit handler: ImplicitInvestigationHandler[F],
      decoderPing: _root_.io.circe.Decoder[Ping],
      encoderPong: _root_.io.circe.Encoder[Pong],
      F: _root_.cats.effect.Sync[F]): _root_.higherkindness.mu.http.protocol.RouteMap[F] =
    _root_.higherkindness.mu.http.protocol
      .RouteMap[F]("ImplicitInvestigation", new ImplicitInvestigationRestService[F]().service)

  // END MACRO GENERATED

  val implicitInvestigationRoute: RouteMap[IO] = routeCreator[IO]

  val ec                            = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]     = IO.timer(ec)

  val server: BlazeServerBuilder[IO] = HttpServer.bind(port, host, implicitInvestigationRoute)

  var serverTask: Fiber[IO, Nothing] = _ // :-(
  before(serverTask = server.resource.use(_ => IO.never).start.unsafeRunSync())
  after(serverTask.cancel)

  "This test" should {

    val client = ImplicitInvestigation.httpClient[IO](serviceUri)

    "work" in {
      val response: IO[Pong] = BlazeClientBuilder[IO](ec).resource.use(client.doPing(Ping(1)))

      response.unsafeRunSync() shouldBe Pong(2)
    }

  }
}
