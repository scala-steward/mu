package higherkindness.mu.rpc.benchmarks.shared.protocols

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import higherkindness.mu.rpc.ChannelFor
import higherkindness.mu.rpc.benchmarks.shared.models.{Person, PersonId, PersonLinkList, PersonList}
import higherkindness.mu.rpc.channel.{ManagedChannelConfig, ManagedChannelInterpreter, UsePlaintext}
import higherkindness.mu.rpc.internal.client.{unaryCalls => unaryClientcalls}
import higherkindness.mu.rpc.internal.server.{unaryCalls => unaryServercalls}
import higherkindness.mu.rpc.internal.service.GRPCServiceDefBuilder
import higherkindness.mu.rpc.protocol.Empty
import io.grpc.stub.{AbstractStub, ServerCalls}
import io.grpc.{CallOptions, Channel, ManagedChannel, MethodDescriptor, ServerServiceDefinition}

import scala.language.higherKinds

trait PersonServiceAvroExpanded[F[_]] {
  def listPersons(empty: Empty.type): F[PersonList]
  def getPerson(id: PersonId): F[Person]
  def getPersonLinks(id: PersonId): F[PersonLinkList]
  def createPerson(person: Person): F[Person]
}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.Throw"))
object PersonServiceAvroExpanded {
  @SuppressWarnings(
    Array("org.wartremover.warts.Null", "org.wartremover.warts.ExplicitImplicitTypes"))
  def listPersonsMethodDescriptor(
      implicit ReqM: MethodDescriptor.Marshaller[Empty.type],
      RespM: MethodDescriptor.Marshaller[PersonList]): MethodDescriptor[Empty.type, PersonList] =
    MethodDescriptor
      .newBuilder(ReqM, RespM)
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(
        MethodDescriptor.generateFullMethodName("PersonServiceAvro", "listPersons"))
      .build()

  @SuppressWarnings(
    Array("org.wartremover.warts.Null", "org.wartremover.warts.ExplicitImplicitTypes"))
  def getPersonMethodDescriptor(
      implicit ReqM: MethodDescriptor.Marshaller[PersonId],
      RespM: MethodDescriptor.Marshaller[Person]): MethodDescriptor[PersonId, Person] =
    MethodDescriptor
      .newBuilder(ReqM, RespM)
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName("PersonServiceAvro", "getPerson"))
      .build()

  @SuppressWarnings(
    Array("org.wartremover.warts.Null", "org.wartremover.warts.ExplicitImplicitTypes"))
  def getPersonLinksMethodDescriptor(
      implicit ReqM: MethodDescriptor.Marshaller[PersonId],
      RespM: MethodDescriptor.Marshaller[PersonLinkList]): MethodDescriptor[
    PersonId,
    PersonLinkList] =
    MethodDescriptor
      .newBuilder(ReqM, RespM)
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(
        MethodDescriptor.generateFullMethodName("PersonServiceAvro", "getPersonLinks"))
      .build()

  @SuppressWarnings(
    Array("org.wartremover.warts.Null", "org.wartremover.warts.ExplicitImplicitTypes"))
  def createPersonMethodDescriptor(
      implicit ReqM: MethodDescriptor.Marshaller[Person],
      RespM: MethodDescriptor.Marshaller[Person]): MethodDescriptor[Person, Person] =
    MethodDescriptor
      .newBuilder(ReqM, RespM)
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(
        MethodDescriptor.generateFullMethodName("PersonServiceAvro", "createPerson"))
      .build()

  def bindService[F[_]](
      implicit CE: ConcurrentEffect[F],
      algebra: PersonServiceAvroExpanded[F]): F[ServerServiceDefinition] =
    GRPCServiceDefBuilder.build[F](
      "PersonServiceAvro",
      scala.Tuple2(
        listPersonsMethodDescriptor,
        ServerCalls.asyncUnaryCall(unaryServercalls.unaryMethod(algebra.listPersons, None))
      ),
      scala.Tuple2(
        getPersonMethodDescriptor,
        ServerCalls.asyncUnaryCall(unaryServercalls.unaryMethod(algebra.getPerson, None))
      ),
      scala.Tuple2(
        getPersonLinksMethodDescriptor,
        ServerCalls.asyncUnaryCall(unaryServercalls.unaryMethod(algebra.getPersonLinks, None))
      ),
      scala.Tuple2(
        createPersonMethodDescriptor,
        ServerCalls.asyncUnaryCall(unaryServercalls.unaryMethod(algebra.createPerson, None))
      )
    )

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments")) class Client[F[_]](
      channel: Channel,
      options: CallOptions)(implicit CE: ConcurrentEffect[F], CS: ContextShift[F])
      extends AbstractStub[Client[F]](channel, options)
      with PersonServiceAvroExpanded[F] {

    override def build(channel: Channel, options: CallOptions): Client[F] =
      new Client[F](channel, options)
    def listPersons(input: Empty.type): F[PersonList] =
      unaryClientcalls.unary(input, listPersonsMethodDescriptor, channel, options)
    def getPerson(input: PersonId): F[Person] =
      unaryClientcalls.unary(input, getPersonMethodDescriptor, channel, options)
    def getPersonLinks(input: PersonId): F[PersonLinkList] =
      unaryClientcalls.unary(input, getPersonLinksMethodDescriptor, channel, options)
    def createPerson(input: Person): F[Person] =
      unaryClientcalls.unary(input, createPersonMethodDescriptor, channel, options)
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def client[F[_]](
      channelFor: ChannelFor,
      channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext()),
      options: CallOptions = CallOptions.DEFAULT)(
      implicit CE: ConcurrentEffect[F],
      CS: ContextShift[F]): Resource[F, PersonServiceAvroExpanded[F]] =
    Resource
      .make(new ManagedChannelInterpreter[F](channelFor, channelConfigList).build)(channel =>
        CE.void(CE.delay(channel.shutdown())))
      .flatMap(ch =>
        Resource.make[F, PersonServiceAvroExpanded[F]](CE.delay(new Client[F](ch, options)))(_ =>
          CE.unit))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def clientFromChannel[F[_]](
      channel: F[ManagedChannel],
      options: CallOptions = CallOptions.DEFAULT)(
      implicit CE: ConcurrentEffect[F],
      CS: ContextShift[F]): Resource[F, PersonServiceAvroExpanded[F]] =
    Resource
      .make(channel)(channel => CE.void(CE.delay(channel.shutdown())))
      .flatMap(ch =>
        Resource.make[F, PersonServiceAvroExpanded[F]](CE.delay(new Client[F](ch, options)))(_ =>
          CE.unit))

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def unsafeClient[F[_]](
      channelFor: ChannelFor,
      channelConfigList: List[ManagedChannelConfig] = List(UsePlaintext()),
      options: CallOptions = CallOptions.DEFAULT)(
      implicit CE: ConcurrentEffect[F],
      CS: ContextShift[F]): PersonServiceAvroExpanded[F] = {

    val managedChannelInterpreter =
      new ManagedChannelInterpreter[F](channelFor, channelConfigList).unsafeBuild

    new Client[F](managedChannelInterpreter, options)
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def unsafeClientFromChannel[F[_]](channel: Channel, options: CallOptions = CallOptions.DEFAULT)(
      implicit CE: ConcurrentEffect[F],
      CS: ContextShift[F]): PersonServiceAvroExpanded[F] = new Client[F](channel, options)
}
