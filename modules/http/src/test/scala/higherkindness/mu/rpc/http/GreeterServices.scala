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

import higherkindness.mu.http.protocol._
import higherkindness.mu.rpc.protocol._
import org.http4s.Method._

@message final case class HelloRequest(hello: String)

@message final case class HelloResponse(hello: String)

@message final case class EmptyResponse()

@service(Avro) trait UnaryGreeter[F[_]] {

  @http4s(GET, MuPath(MuPathSegment("hello"), MuPathMatcher))
  def getHello(request: Empty.type): F[HelloResponse]
}
