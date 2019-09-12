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

package higherkindness.mu.rpc.kafka

import cats.effect.{ConcurrentEffect, ContextShift, IO, Resource, Sync}
import fs2.kafka._
import higherkindness.mu.rpc.protocol.Empty
import higherkindness.mu.rpc.testing.servers.withServerChannel
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.admin.AdminClientConfig
import org.scalatest._

import scala.concurrent.ExecutionContext

import KafkaManagementService._

class ServiceSpec extends FunSuite with Matchers with OneInstancePerTest with EmbeddedKafka {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  def adminClientSettings[F[_]: Sync](config: EmbeddedKafkaConfig): AdminClientSettings[F] =
    AdminClientSettings[F].withProperties(
      Map(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> s"localhost:${config.kafkaPort}")
    )

  def withKafka[F[_]: Sync, A](f: AdminClientSettings[F] => A): A =
    withRunningKafkaOnFoundPort(
      EmbeddedKafkaConfig()
    )(adminClientSettings[F] _ andThen f)

  def withClient[F[_]: ContextShift: ConcurrentEffect, A](
      settings: AdminClientSettings[F]
  )(f: KafkaManagement[F] => F[A]): F[A] = {
    val client: Resource[F, KafkaManagement[F]] = for {
      km            <- KafkaManagement.buildInstance[F](settings)
      serverChannel <- withServerChannel(KafkaManagement.bindService[F](ConcurrentEffect[F], km))
      client        <- KafkaManagement.clientFromChannel[F](Sync[F].delay(serverChannel.channel))
    } yield client

    client.use(c => f(c))
  }

  test("list topics") {
    withKafka { settings: AdminClientSettings[IO] =>
      withClient(settings) { client =>
        for {
          list <- client.listTopics(Empty).attempt
          _    <- IO(println(list))
          _    <- IO(assert(list.isRight))
        } yield ()
      }.unsafeRunSync()
    }
  }
}
