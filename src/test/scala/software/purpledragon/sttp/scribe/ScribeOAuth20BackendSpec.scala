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

import com.github.scribejava.core.exceptions.OAuthException
import com.github.scribejava.core.model.{OAuth2AccessToken, OAuthRequest, Verb}
import com.github.scribejava.core.oauth.OAuth20Service
import org.scalamock.matchers.ArgCapture.CaptureAll
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client._
import sttp.model.{MediaType, StatusCode}

class ScribeOAuth20BackendSpec extends AnyFlatSpec with Matchers with MockFactory with ScribeHelpers {

  "ScribeOAuth20Backend" should "send get request" in new ScribeOAuth20Fixture {
    // given
    stubResponses(
      StringBodyResponse("OK")
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("OK")

    verifyRequests(
      RequestExpectation("https://example.com/api/test")
    )
  }

  it should "send get request with query params" in new ScribeOAuth20Fixture {
    // given
    stubResponses(
      StringBodyResponse("OK")
    )

    val qString = "query"

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test?q=$qString&f=5")
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("OK")

    verifyRequests(
      RequestExpectation("https://example.com/api/test?q=query&f=5")
    )
  }

  it should "send post request" in new ScribeOAuth20Fixture {
    // given
    stubResponses(
      StringBodyResponse("""{ "user": { "id": 1, "name": "John" }}""")
    )

    val requestBody = """{ "user": { "name": "John" }}"""

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .post(uri"https://example.com/api/test")
      .body(requestBody)
      .contentType(MediaType.ApplicationJson)
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("""{ "user": { "id": 1, "name": "John" }}""")

    verifyRequests(
      RequestExpectation(
        "https://example.com/api/test",
        verb = Verb.POST,
        headers = Map(
          "Accept-Encoding" -> "gzip, deflate",
          "Content-Type" -> "application/json",
          "Content-Length" -> requestBody.length.toString
        ),
        body = Some(requestBody)
      )
    )
  }

  it should "pass through 404 response" in new ScribeOAuth20Fixture {
    // given
    stubResponses(
      StringBodyResponse("Test not found", status = StatusNotFound)
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.NotFound
    result.body shouldBe Left("Test not found")

    verifyRequests(
      RequestExpectation("https://example.com/api/test")
    )
  }

  it should "refresh token on 401" in new ScribeOAuth20Fixture {
    // given
    (tokenProvider.tokenRenewed _).expects(*)
    (oauthService.refreshAccessToken(_: String)).expects("refresh-token").returning(updatedToken)
    (oauthService.signRequest(_: OAuth2AccessToken, _: OAuthRequest)).expects(updatedToken, capture(requestCaptor))

    stubResponses(
      StringBodyResponse(
        """{"error": "invalid_token","error_description": "The access token expired"}""",
        status = StatusUnauthorized
      ),
      StringBodyResponse("OK")
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("OK")

    verifyRequests(
      RequestExpectation("https://example.com/api/test"),
      RequestExpectation("https://example.com/api/test")
    )
  }

  it should "return 401 on token refresh failure" in new ScribeOAuth20Fixture {
    // given
    (oauthService.refreshAccessToken(_: String)).expects("refresh-token").throws(new OAuthException("Failed"))

    stubResponses(
      StringBodyResponse(
        """{"error": "invalid_token","error_description": "The access token expired"}""",
        status = StatusUnauthorized
      )
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Unauthorized
    result.body shouldBe Left("""{"error": "invalid_token","error_description": "The access token expired"}""")

    verifyRequests(
      RequestExpectation("https://example.com/api/test")
    )
  }

  "ScribeOAuth20Backend(encodingStyle = Scribe)" should "send get request with query params" in new ScribeOAuth20Fixture {
    // given
    override protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      new ScribeOAuth20Backend(oauthService, tokenProvider, encodingStyle = QueryParamEncodingStyle.Scribe)

    stubResponses(
      StringBodyResponse("OK")
    )

    val qString = "query"

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test?q=$qString&f=5")
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("OK")

    verifyRequests(
      RequestExpectation("https://example.com/api/test", queryParams = Map("q" -> "query", "f" -> "5"))
    )
  }

  "ScribeOAuth20Backend(grantType = ClientCredentials)" should "refresh token on 401" in new ScribeOAuth20Fixture {
    // given
    override protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      new ScribeOAuth20Backend(oauthService, tokenProvider, grantType = OAuth2GrantType.ClientCredentials)

    (tokenProvider.tokenRenewed _).expects(*)
    (oauthService.getAccessTokenClientCredentialsGrant: () => OAuth2AccessToken).expects().returning(updatedToken)
    (oauthService.signRequest(_: OAuth2AccessToken, _: OAuthRequest)).expects(updatedToken, capture(requestCaptor))

    stubResponses(
      StringBodyResponse(
        """{"error": "invalid_token","error_description": "The access token expired"}""",
        status = StatusUnauthorized
      ),
      StringBodyResponse("OK")
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Ok
    result.body shouldBe Right("OK")

    verifyRequests(
      RequestExpectation("https://example.com/api/test"),
      RequestExpectation("https://example.com/api/test")
    )
  }

  it should "return 401 on token refresh failure" in new ScribeOAuth20Fixture {
    // given
    override protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      new ScribeOAuth20Backend(oauthService, tokenProvider, grantType = OAuth2GrantType.ClientCredentials)

    (oauthService.getAccessTokenClientCredentialsGrant: () => OAuth2AccessToken)
      .expects()
      .throws(new OAuthException("Failed"))

    stubResponses(
      StringBodyResponse(
        """{"error": "invalid_token","error_description": "The access token expired"}""",
        status = StatusUnauthorized
      )
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Unauthorized
    result.body shouldBe Left("""{"error": "invalid_token","error_description": "The access token expired"}""")

    verifyRequests(
      RequestExpectation("https://example.com/api/test")
    )
  }

  private trait ScribeOAuth20Fixture {
    val oauthService: OAuth20Service = mock[OAuth20Service]
    val tokenProvider: OAuth2TokenProvider = mock[OAuth2TokenProvider]

    val accessToken: OAuth2AccessToken = new OAuth2AccessToken("access-token", null, null, "refresh-token", null, null)
    val updatedToken: OAuth2AccessToken = new OAuth2AccessToken("updated-token")

    val requestCaptor: CaptureAll[OAuthRequest] = CaptureAll[OAuthRequest]()

    // common request parts
    (tokenProvider.accessTokenForRequest _).expects().returning(accessToken).once()
    (oauthService.signRequest(_: OAuth2AccessToken, _: OAuthRequest)).expects(accessToken, capture(requestCaptor))

    protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      new ScribeOAuth20Backend(oauthService, tokenProvider)

    protected def stubResponses(responses: TestResponse*): Unit = {
      responses foreach { response =>
        (oauthService.execute(_: OAuthRequest)).expects(*).returning(response.toResponse)
      }
    }

    protected def verifyRequests(requests: RequestExpectation*): Unit = {
      requestCaptor.values should have size requests.size

      requestCaptor.values.zip(requests) foreach {
        case (request, expected) =>
          expected.verify(request)
      }
    }
  }
}
