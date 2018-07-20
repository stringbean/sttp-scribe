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

import com.github.scribejava.core.model.{OAuth1AccessToken, OAuthRequest}
import com.github.scribejava.core.oauth.OAuth10aService

class ScribeOAuth10aBackend(service: OAuth10aService, tokenProvider: OAuth1TokenProvider)
    extends ScribeBackend(service) {

  override protected def signRequest(request: OAuthRequest): Unit = {
    service.signRequest(tokenProvider.accessTokenForRequest, request)
  }
}

trait OAuth1TokenProvider {
  def accessTokenForRequest: OAuth1AccessToken
}
