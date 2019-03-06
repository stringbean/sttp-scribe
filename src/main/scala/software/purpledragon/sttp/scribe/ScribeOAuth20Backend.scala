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

class ScribeOAuth20Backend(service: OAuth20Service, tokenProvider: OAuth2TokenProvider) extends ScribeBackend(service) {
  private var oauthToken: Option[OAuth2AccessToken] = None

  override protected def signRequest(request: OAuthRequest): Unit = {
    service.signRequest(currentToken, request)
  }

  override protected def renewAccessToken(response: Response): Boolean = {
    try {
      val newToken = service.refreshAccessToken(currentToken.getRefreshToken)
      tokenProvider.tokenRenewed(newToken)
      oauthToken = Some(newToken)
      true
    } catch {
      case _: OAuthException =>
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

trait OAuth2TokenProvider {
  def accessTokenForRequest: OAuth2AccessToken
  def tokenRenewed(newToken: OAuth2AccessToken): Unit
}
