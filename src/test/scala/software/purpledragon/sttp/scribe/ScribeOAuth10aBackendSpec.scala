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

import com.github.scribejava.core.builder.api.DefaultApi10a
import com.github.scribejava.core.model.{OAuth1AccessToken, OAuthRequest, Verb}
import com.github.scribejava.core.oauth.OAuth10aService
import org.mockito.ArgumentMatchersSugar._
import org.mockito.MockitoSugar._
import org.mockito.captor.{ArgCaptor, Captor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client._
import sttp.model.{MediaType, StatusCode}

class ScribeOAuth10aBackendSpec extends AnyFlatSpec with Matchers with ScribeHelpers {

  "ScribeOAuth10aBackend" should "send get request" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse("OK")
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

  it should "send get request with query params" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse("OK")
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

  it should "send post request" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse("""{ "user": { "id": 1, "name": "John" }}""")
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

  it should "pass through 404 response" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse("Test not found", status = StatusNotFound)
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

  it should "refresh token on 401 token expired" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse(
        "oauth_problem=token_expired&oauth_problem_advice=The access token has expired.",
        status = StatusUnauthorized
      ),
      StringResponse("oauth_token=updated-token&oauth_token_secret=updated-secret"),
      StringResponse("OK")
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
      RequestExpectation("https://example.com/oauth/access-token", headers = Map.empty),
      RequestExpectation("https://example.com/api/test")
    )

    verify(tokenProvider).prepareTokenRenewalRequest(any[OAuthRequest])
    verify(tokenProvider).tokenRenewed(any[OAuth1AccessToken])
  }

  it should "only refresh token once" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse(
        "oauth_problem=token_expired&oauth_problem_advice=The access token has expired.",
        status = StatusUnauthorized
      ),
      StringResponse("oauth_token=updated-token&oauth_token_secret=updated-secret"),
      StringResponse(
        "oauth_problem=token_expired&oauth_problem_advice=The access token has expired.",
        status = StatusUnauthorized
      )
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Unauthorized
    result.body shouldBe Left("oauth_problem=token_expired&oauth_problem_advice=The access token has expired.")

    verifyRequests(
      RequestExpectation("https://example.com/api/test"),
      RequestExpectation("https://example.com/oauth/access-token", headers = Map.empty),
      RequestExpectation("https://example.com/api/test")
    )

    verify(tokenProvider).prepareTokenRenewalRequest(any[OAuthRequest])
    verify(tokenProvider).tokenRenewed(any[OAuth1AccessToken])
  }

  it should "return 401 if not token expired error" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse(
        "access denied",
        status = StatusUnauthorized
      )
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Unauthorized
    result.body shouldBe Left("access denied")

    verifyRequests(
      RequestExpectation("https://example.com/api/test")
    )
  }

  it should "return 401 on token refresh failure" in new ScribeOAuth10aFixture {
    // given
    stubResponses(
      StringResponse(
        "oauth_problem=token_expired&oauth_problem_advice=The access token has expired.",
        status = StatusUnauthorized
      ),
      StringResponse(
        "token renew failed",
        status = StatusUnauthorized
      )
    )

    // when
    val result: Identity[Response[Either[String, String]]] = basicRequest
      .get(uri"https://example.com/api/test")
      .send()

    // then
    result.code shouldBe StatusCode.Unauthorized
    result.body shouldBe Left("oauth_problem=token_expired&oauth_problem_advice=The access token has expired.")

    verifyRequests(
      RequestExpectation("https://example.com/api/test"),
      RequestExpectation("https://example.com/oauth/access-token", headers = Map.empty)
    )

    verify(tokenProvider).prepareTokenRenewalRequest(any[OAuthRequest])
    verify(tokenProvider, never).tokenRenewed(any[OAuth1AccessToken])
  }

  "ScribeOAuth10aBackend(encodingStyle = Scribe)" should "send get request with query params" in
    new ScribeOAuth10aFixture {
      // given
      override protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
        new ScribeOAuth10aBackend(oauthService, tokenProvider, encodingStyle = QueryParamEncodingStyle.Scribe)

      stubResponses(
        StringResponse("OK")
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

  private trait ScribeOAuth10aFixture {
    val oauthService: OAuth10aService = mock[OAuth10aService]
    val tokenProvider: OAuth1TokenProvider = mock[OAuth1TokenProvider]
    val oauthApi: DefaultApi10a = new DefaultApi10a() {
      override val getRequestTokenEndpoint: String = "https://example.com/oauth/request-token"
      override val getAccessTokenEndpoint: String = "https://example.com/oauth/access-token"
      override val getAuthorizationBaseUrl: String = "https://example.com/oauth/authorization"
    }

    val accessToken: OAuth1AccessToken = new OAuth1AccessToken("access-token", "token-secret")

    val requestCaptor: Captor[OAuthRequest] = ArgCaptor[OAuthRequest]

    // common to all requests
    doReturn(accessToken).when(tokenProvider).accessTokenForRequest
    doNothing.when(oauthService).signRequest(any[OAuth1AccessToken], requestCaptor.capture)
    doReturn(oauthApi).when(oauthService).getApi

    protected implicit val backend: SttpBackend[Identity, Nothing, NothingT] =
      new ScribeOAuth10aBackend(oauthService, tokenProvider)

    protected def stubResponse(response: TestResponse): Unit = {
      doReturn(response.toResponse).when(oauthService).execute(any[OAuthRequest])
    }

    protected def stubResponses(responses: TestResponse*): Unit = {
      val scribeResponses = responses.map(_.toResponse)
      doReturn(scribeResponses.head, scribeResponses.tail: _*).when(oauthService).execute(any[OAuthRequest])
    }

    protected def verifyRequests(requests: RequestExpectation*): Unit = {
      requestCaptor.values should have size requests.size

      requestCaptor.values.zip(requests) foreach { case (request, expected) =>
        expected.verify(request)
      }
    }
  }
}
