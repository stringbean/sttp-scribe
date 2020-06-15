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

import com.github.scribejava.core.model.OAuth2AccessToken
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OAuth2TokenProviderSpec extends AnyFlatSpec with Matchers {

  "OAuth2TokenProvider.basicProviderFor" should "create a provider that maintains a token" in {
    val token1 = new OAuth2AccessToken("token-1")
    val token2 = new OAuth2AccessToken("token-2")

    val provider = OAuth2TokenProvider.basicProviderFor(token1)
    provider.accessTokenForRequest shouldBe token1

    provider.tokenRenewed(token2)
    provider.accessTokenForRequest shouldBe token2
  }
}
