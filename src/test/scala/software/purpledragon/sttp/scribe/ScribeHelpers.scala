/*
 * Copyright 2018 Michael Stringer
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

package software.purpledragon.sttp.scribe

import java.nio.charset.{Charset, StandardCharsets}
import com.github.scribejava.core.model.{OAuthRequest, ParameterList, Response => ScribeResponse, Verb}
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers

import org.mockito.ArgumentMatchersSugar._
import org.mockito.MockitoSugar._
import scala.jdk.CollectionConverters._

trait ScribeHelpers extends Matchers {
  this: Suite =>

  type ResponseStatus = (Int, String)
  val StatusOk: ResponseStatus = (200, "OK")
  val StatusNotFound: ResponseStatus = (404, "Not Found")
  val StatusUnauthorized: ResponseStatus = (401, "Unauthorized")

  case class RequestExpectation(
      url: String,
      verb: Verb = Verb.GET,
      queryParams: Map[String, String] = Map.empty,
      headers: Map[String, String] = Map("Accept-Encoding" -> "gzip, deflate"),
      body: Option[String] = None
  ) {

    def verify(request: OAuthRequest): Unit = {
      request.getUrl shouldBe url
      request.getVerb shouldBe verb
      request.getHeaders.asScala shouldBe headers

      // request.getQueryStringParams parses the url so we need to access the params directly
      val actualParams = FieldUtils
        .readField(request, "querystringParams", true)
        .asInstanceOf[ParameterList]
        .getParams
        .asScala
        .map(param => param.getKey -> param.getValue)
        .toMap

      actualParams shouldBe queryParams

      body match {
        case Some(payload) => request.getStringPayload shouldBe payload
        case None => request.getStringPayload shouldBe null
      }
    }
  }

  trait TestResponse {
    val status: ResponseStatus
    val headers: Map[String, String]

    def toResponse: ScribeResponse = {
      val response: ScribeResponse = mock[ScribeResponse]

      doReturn(status._1).when(response).getCode
      doReturn(status._2).when(response).getMessage

      doReturn(headers.asJava).when(response).getHeaders
      doAnswer { name: String =>
        headers.getOrElse(name, null)
      }.when(response).getHeader(any[String])

      stubBody(response)

      response
    }

    protected def stubBody(response: ScribeResponse): Unit
  }

  case class StringResponse(
      body: String,
      status: ResponseStatus = StatusOk,
      headers: Map[String, String] = Map.empty,
      charset: Charset = StandardCharsets.UTF_8
  ) extends TestResponse {

    override protected def stubBody(response: ScribeResponse): Unit = {
      val stream = IOUtils.toInputStream(body, charset)

      // some calls are via the stream, others via the string body
      doReturn(stream).when(response).getStream
      doReturn(body).when(response).getBody
    }
  }
}
