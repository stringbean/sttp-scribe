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

import com.github.scribejava.core.model.{OAuth1AccessToken, OAuthRequest, Response, Verb}
import com.github.scribejava.core.oauth.OAuth10aService
import com.github.scribejava.core.exceptions.OAuthException
import software.purpledragon.sttp.scribe.QueryParamEncodingStyle.Sttp

class ScribeOAuth10aBackend(service: OAuth10aService,
                            tokenProvider: OAuth1TokenProvider,
                            encodingStyle: QueryParamEncodingStyle = Sttp) extends ScribeBackend(service) with Logging {

  private var oauthToken: Option[OAuth1AccessToken] = None

  override final def withEncodingStyle(style: QueryParamEncodingStyle): ScribeOAuth10aBackend = {
    new ScribeOAuth10aBackend(service, tokenProvider, style)
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
        logger.warn("Error while renewing OAuth token")
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
