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
import com.github.scribejava.core.model.{OAuth1AccessToken, OAuthRequest, Response, Verb}
import com.github.scribejava.core.oauth.OAuth10aService
import software.purpledragon.sttp.scribe.QueryParamEncodingStyle.Sttp

object ScribeOAuth10aBackend {
  private val TokenExpiredPattern = ".*oauth_problem=token_expired.*".r

  val DefaultTokenExpiredCheck: TokenExpiredResponseCheck = { response =>
    response.getBody match {
      case TokenExpiredPattern() => true
      case _ => false
    }
  }
}

class ScribeOAuth10aBackend(
    service: OAuth10aService,
    tokenProvider: OAuth1TokenProvider,
    isTokenExpiredResponse: TokenExpiredResponseCheck = ScribeOAuth10aBackend.DefaultTokenExpiredCheck,
    encodingStyle: QueryParamEncodingStyle = Sttp
) extends ScribeBackend(service, isTokenExpiredResponse, encodingStyle)
    with Logging {

  private var oauthToken: Option[OAuth1AccessToken] = None

  override final def withEncodingStyle(style: QueryParamEncodingStyle): ScribeOAuth10aBackend = {
    new ScribeOAuth10aBackend(service, tokenProvider, isTokenExpiredResponse, style)
  }

  override protected def signRequest(request: OAuthRequest): Unit = {
    service.signRequest(currentToken, request)
  }

  override protected def renewAccessToken(response: Response): Boolean = {
    try {
      logger.debug("Renewing access token for request")
      val api = service.getApi
      val request = new OAuthRequest(Verb.GET, api.getAccessTokenEndpoint)

      tokenProvider.prepareTokenRenewalRequest(request)
      service.signRequest(currentToken, request)

      val response = service.execute(request)
      val newToken = api.getAccessTokenExtractor.extract(response)

      tokenProvider.tokenRenewed(newToken)
      oauthToken = Some(newToken)
      true
    } catch {
      case oae: OAuthException =>
        logger.warn("Error while renewing OAuth token: {}", oae.getMessage)
        logger.trace("Error while renewing OAuth token", oae)
        false
    }
  }

  private def currentToken: OAuth1AccessToken = {
    if (oauthToken.isEmpty) {
      oauthToken = Some(tokenProvider.accessTokenForRequest)
    }

    oauthToken.get
  }
}

trait OAuth1TokenProvider extends OAuthTokenProvider[OAuth1AccessToken] {

  /**
    * Add any additional required parameters to a request to renew an access token.
    */
  def prepareTokenRenewalRequest(request: OAuthRequest): Unit = ()
}

object OAuth1TokenProvider {

  /**
    * Basic [[OAuth1TokenProvider]] for situations where you don't need to store any renewed tokens. Think *very*
    * carefully before using this token provider!
    *
    * @param token initial access token to use.
    */
  def basicProviderFor(token: OAuth1AccessToken): OAuth1TokenProvider = {
    new OAuth1TokenProvider() {
      private var current: OAuth1AccessToken = token

      override def accessTokenForRequest: OAuth1AccessToken = current
      override def tokenRenewed(newToken: OAuth1AccessToken): Unit = current = newToken
    }
  }
}
