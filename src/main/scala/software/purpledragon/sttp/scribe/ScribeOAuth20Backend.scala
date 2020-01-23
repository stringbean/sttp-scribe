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
import com.github.scribejava.core.model.{OAuth2AccessToken, OAuthRequest, Response}
import com.github.scribejava.core.oauth.OAuth20Service
import software.purpledragon.sttp.scribe.QueryParamEncodingStyle.Sttp

class ScribeOAuth20Backend(
    service: OAuth20Service,
    tokenProvider: OAuth2TokenProvider,
    grantType: OAuth2GrantType = OAuth2GrantType.AuthorizationCode,
    encodingStyle: QueryParamEncodingStyle = Sttp
) extends ScribeBackend(service, encodingStyle)
    with Logging {

  private var oauthToken: Option[OAuth2AccessToken] = None

  override final def withEncodingStyle(style: QueryParamEncodingStyle): ScribeOAuth20Backend = {
    new ScribeOAuth20Backend(service, tokenProvider, grantType, style)
  }

  override protected def signRequest(request: OAuthRequest): Unit = {
    service.signRequest(currentToken, request)
  }

  override protected def renewAccessToken(response: Response): Boolean = {
    try {
      logger.debug("Renewing access token for request")

      val newToken = grantType match {
        case OAuth2GrantType.AuthorizationCode =>
          service.refreshAccessToken(currentToken.getRefreshToken)
        case OAuth2GrantType.ClientCredentials =>
          service.getAccessTokenClientCredentialsGrant()
      }
      tokenProvider.tokenRenewed(newToken)
      oauthToken = Some(newToken)
      true
    } catch {
      case oae: OAuthException =>
        logger.warn("Error while renewing OAuth token", oae)
        false
    }
  }

  private def currentToken: OAuth2AccessToken = {
    if (oauthToken.isEmpty) {
      oauthToken = Some(tokenProvider.accessTokenForRequest)
    }

    oauthToken.get
  }
}

trait OAuth2TokenProvider extends OAuthTokenProvider[OAuth2AccessToken]

object OAuth2TokenProvider {

  /**
    * Basic [[OAuth2TokenProvider]] for situations where you don't need to store any renewed tokens. Think *very*
    * carefully before using this token provider!
    *
    * @param token initial access token to use.
    */
  def basicProviderFor(token: OAuth2AccessToken): OAuth2TokenProvider = {
    new OAuth2TokenProvider() {
      private var current: OAuth2AccessToken = token

      override def accessTokenForRequest: OAuth2AccessToken = current
      override def tokenRenewed(newToken: OAuth2AccessToken): Unit = current = newToken
    }
  }
}

sealed trait OAuth2GrantType

object OAuth2GrantType {
  case object AuthorizationCode extends OAuth2GrantType
  case object ClientCredentials extends OAuth2GrantType
}
