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

package higherkindness.mu.rpc.kafka.handler

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits._
import fs2.kafka._
import higherkindness.mu.rpc.kafka.KafkaManagementService._
import higherkindness.mu.rpc.protocol.Empty
import org.apache.kafka.clients.admin.NewPartitions
import org.apache.kafka.clients.admin.NewTopic
import higherkindness.mu.rpc.kafka.KafkaManagementService

object KafkaManagement {
  def buildInstance[F[_]: ContextShift: Concurrent](): Resource[F, KafkaManagement[F]] =
    adminClientResource[F](AdminClientSettings.apply[F])
      .map(c => new KafkaManagementImpl(c))

  class KafkaManagementImpl[F[_]: ContextShift: Concurrent](
      adminClient: KafkaAdminClient[F]
  ) extends KafkaManagement[F] {
    override def createPartitions(cpr: CreatePartitionsRequest): F[Unit] =
      adminClient.createPartitions(cpr.ps.mapValues(NewPartitions.increaseTo))

    override def createTopic(ctr: CreateTopicRequest): F[Unit] =
      adminClient.createTopic(new NewTopic(ctr.name, ctr.numPartitions, ctr.replicationFactor))
    override def createTopics(ctrs: List[CreateTopicRequest]): F[Unit] =
      adminClient.createTopics(ctrs.map { ctr =>
        new NewTopic(ctr.name, ctr.numPartitions, ctr.replicationFactor)
      })

    override def deleteTopic(t: String): F[Unit]         = adminClient.deleteTopic(t)
    override def deleteTopics(ts: List[String]): F[Unit] = adminClient.deleteTopics(ts)

    override def describeCluster(r: Empty.type): F[Cluster] = {
      val dc = adminClient.describeCluster
      (dc.clusterId, dc.controller, dc.nodes).mapN { (id, c, ns) =>
        Cluster(ns.map(Node.fromKafkaNode).toList, Node.fromKafkaNode(c), id)
      }
    }

    override def describeConfigs(rs: List[ConfigResource]): F[Configs] = for {
      kConfigs <- adminClient.describeConfigs(rs.map(ConfigResource.toKafkaConfigResource))
      configs = kConfigs.map { case (cr, ces) =>
          ConfigResource.fromKafkaConfigResource(cr) ->
            ces.map(ConfigEntry.fromKafkaConfigEntry)
      }
    } yield Configs(configs)

    override def describeConsumerGroups(groupIds: List[String]): F[ConsumerGroups] = for {
      kGroups <- adminClient.describeConsumerGroups(groupIds)
      groups = kGroups.map { case (gid, cgd) =>
        gid -> ConsumerGroupDescription.fromKafkaConsumerGroupDescription(cgd)
      }
    } yield ConsumerGroups(groups)

    override def describeTopics(topics: List[String]): F[KafkaManagementService.Topics] = for {
      kTopics <- adminClient.describeTopics(topics)
      topics = kTopics.map { case (topic, desc) =>
        topic -> TopicDescription.fromKafkaTopicDescription(desc)
      }
    } yield Topics(topics)
  }
}
