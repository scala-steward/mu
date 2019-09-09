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

import higherkindness.mu.rpc.protocol.{service, Empty}
import org.apache.kafka.common.{Node => KNode}
import org.apache.kafka.common.config.{ConfigResource => KConfigResource}
import org.apache.kafka.clients.admin.{ConfigEntry => KConfigEntry}

import scala.collection.JavaConverters._

object KafkaManagementService {
  final case class CreatePartitionsRequest(ps: Map[String, Int])

  @service(Protobuf)
  trait KafkaManagement[F[_]] {
    def createPartitions(cpr: CreatePartitionsRequest): F[Unit]
  }
}
