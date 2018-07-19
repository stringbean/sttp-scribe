package software.purpledragon.sttp.scribe

import com.github.scribejava.core.model.{OAuth2AccessToken, OAuthRequest}
import com.github.scribejava.core.oauth.OAuth20Service

class ScribeOAuth20Backend(service: OAuth20Service) extends ScribeBackend(service) {

  override protected def signRequest(request: OAuthRequest): Unit = {
    val accessToken: OAuth2AccessToken = ???
    service.signRequest(accessToken, request)
  }
}
