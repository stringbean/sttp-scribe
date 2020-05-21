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

import java.io.{FileOutputStream, InputStream, UnsupportedEncodingException}
import java.net.URLDecoder
import java.util.zip.{GZIPInputStream, InflaterInputStream}

import com.github.scribejava.core.model.{OAuthRequest, Token, Verb, Response => ScribeResponse}
import com.github.scribejava.core.oauth.OAuthService
import software.purpledragon.sttp.scribe.QueryParamEncodingStyle._
import sttp.client._
import sttp.client.monad.{IdMonad, MonadError}
import sttp.client.ws.WebSocketResponse
import sttp.model._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.higherKinds
import scala.util.Using

abstract class ScribeBackend(service: OAuthService, encodingStyle: QueryParamEncodingStyle = Sttp)
    extends SttpBackend[Identity, Nothing, NothingT] {

  /**
    * Url query parameter encoding is handled slightly differently by sttp and scribe. This allows
    * you to configure which implementation the backend should use.
    */
  def withEncodingStyle(encodingStyle: QueryParamEncodingStyle): ScribeBackend

  override def send[T](request: Request[T, Nothing]): Response[T] = {
    val (url, params) = encodingStyle match {
      case Sttp =>
        (request.uri.toString, Nil)
      case Scribe =>
        (request.uri.copy(querySegments = Nil).toString, request.uri.paramsSeq)
    }
    val oAuthRequest = new OAuthRequest(method2Verb(request.method), url)

    params foreach {
      case (name, value) => oAuthRequest.addQuerystringParameter(name, value)
    }
    request.headers foreach { header =>
      oAuthRequest.addHeader(header.name, header.value)
    }

    val contentType = request.headers
      .find(_.name.equalsIgnoreCase(HeaderNames.ContentType))
      .map(_.value.takeWhile(_ != ';'))
    setRequestPayload(request.body, contentType, oAuthRequest)

    signRequest(oAuthRequest)

    val response = service.execute(oAuthRequest)

    if (response.getCode == StatusCode.Unauthorized.code && renewAccessToken(response)) {
      // renewed access token - retry the request
      send(request)
    } else {
      handleResponse(response, request.response)
    }
  }

  override def openWebsocket[T, WS_RESULT](
      request: Request[T, Nothing],
      handler: NothingT[WS_RESULT]): Identity[WebSocketResponse[WS_RESULT]] = {
    // we don't handle websockets
    handler
  }

  override def close(): Identity[Unit] = ()
  override val responseMonad: MonadError[Identity] = IdMonad

  protected def signRequest(request: OAuthRequest): Unit
  protected def renewAccessToken(response: ScribeResponse): Boolean

  private def handleResponse[T](r: ScribeResponse, responseAs: ResponseAs[T, Nothing]): Response[T] = {
    val statusCode = StatusCode(r.getCode)

    // scribe includes the status line as a header with a key of 'null' :-()
    val headers = r.getHeaders.asScala.toList
      .filterNot(_._1 == null)
      .map(h => Header(h._1, h._2))

    val metadata = ResponseMetadata(headers, statusCode, r.getMessage)
    val contentEncoding = Option(r.getHeader(HeaderNames.ContentEncoding))
    val is = wrapInput(r.getStream, contentEncoding)
    val body = readResponseBody(is, responseAs, metadata)

    Response(body, statusCode, r.getMessage, headers, Nil)
  }

  private def readResponseBody[T](is: InputStream, responseAs: ResponseAs[T, Nothing], meta: ResponseMetadata): T = {
    responseAs match {
      case MappedResponseAs(raw, g) => g(readResponseBody(is, raw, meta), meta)

      case ResponseAsFromMetadata(f) => readResponseBody(is, f(meta), meta)

      case IgnoreResponse =>
        @tailrec def consume(): Unit = if (is.read() != -1) consume()
        consume()

      case ResponseAsByteArray =>
        toByteArray(is)

      case ResponseAsStream() =>
        // only possible when the user requests the response as a stream of
        // Nothing. Oh well ...
        throw new IllegalStateException()

      case ResponseAsFile(output) =>
        val file = output.toFile

        if (!file.exists()) {
          if (file.getParentFile != null) {
            file.getParentFile.mkdirs()
          }
          file.createNewFile()
        }

        Using.resource(new FileOutputStream(file)) { os =>
          transfer(is, os)
        }
        output
    }
  }

  private def encodingFromContentType(contentType: String): Option[String] = {
    contentType
      .split(";")
      .map(_.trim.toLowerCase)
      .collectFirst {
        case s if s.startsWith("charset=") => s.substring(8)
      }
  }

  private def setRequestPayload(body: RequestBody[_], contentType: Option[String], request: OAuthRequest): Unit = {
    body match {
      case StringBody(content, encoding, _)
          if contentType.contains(MediaType.ApplicationXWwwFormUrlencoded.toString()) =>
        // have to add these as "body parameters" so that they get included in the oauth signature
        val FormParam = "(.*)=(.*)".r
        val bodyParams: Seq[(String, String)] = content.split("&").collect {
          case FormParam(key, value) => (URLDecoder.decode(key, encoding), URLDecoder.decode(value, encoding))
        }
        bodyParams.foreach(p => request.addBodyParameter(p._1, p._2))

      case StringBody(content, encoding, _) =>
        request.setPayload(content)
        request.setCharset(encoding)

      case ByteArrayBody(content, _) =>
        request.setPayload(content)

      case ByteBufferBody(content, _) =>
        request.setPayload(content.array())

      case FileBody(content, _) =>
        request.setPayload(content.toFile)

      case InputStreamBody(_, _) =>
        throw new UnsupportedOperationException("scribe does not support InputStream bodies")

      case StreamBody(_) =>
        throw new UnsupportedOperationException("scribe does not support Stream bodies")

      case MultipartBody(_) =>
        throw new UnsupportedOperationException("scribe does not support Multipart bodies")

      case NoBody =>
      // nothing to set
    }
  }

  private def method2Verb(method: Method): Verb = {
    method match {
      case Method.GET => Verb.GET
      case Method.POST => Verb.POST
      case Method.PUT => Verb.PUT
      case Method.DELETE => Verb.DELETE
      case Method.OPTIONS => Verb.OPTIONS
      case Method.PATCH => Verb.PATCH
      case Method.TRACE => Verb.TRACE
      case m => throw new NotImplementedError(s"Scribe does not support $m")
    }
  }

  private def wrapInput(is: InputStream, encoding: Option[String]) = {
    encoding.map(_.toLowerCase) match {
      case None => is
      case Some("gzip") => new GZIPInputStream(is)
      case Some("deflate") => new InflaterInputStream(is)
      case Some(ce) => throw new UnsupportedEncodingException(s"Unsupported encoding: $ce")
    }
  }

}

trait OAuthTokenProvider[T <: Token] {
  def accessTokenForRequest: T
  def tokenRenewed(token: T): Unit
}

sealed trait QueryParamEncodingStyle

object QueryParamEncodingStyle {
  case object Sttp extends QueryParamEncodingStyle
  case object Scribe extends QueryParamEncodingStyle
}
