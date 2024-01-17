# sttp-scribe - sttp backend for ScribeJava


[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/stringbean/sttp-scribe/ci.yml)](https://travis-ci.org/stringbean/sttp-scribe)
[![Codacy Grade](https://img.shields.io/codacy/grade/6becacf763074472b1360c0d41cd8478.svg?label=codacy)](https://www.codacy.com/app/stringbean/sttp-scribe)
[![Test Coverage](https://img.shields.io/codecov/c/github/stringbean/sttp-scribe/master.svg)](https://codecov.io/gh/stringbean/sttp-scribe)
[![Maven Central - Scala 2.11](https://img.shields.io/maven-central/v/software.purpledragon/sttp-scribe_2.11.svg?label=scala%202.11)](https://central.sonatype.com/search?q=a:sttp-scribe_2.11)
[![Maven Central - Scala 2.12](https://img.shields.io/maven-central/v/software.purpledragon/sttp-scribe_2.12.svg?label=scala%202.12)](https://central.sonatype.com/search?q=a:sttp-scribe_2.12)
[![Maven Central - Scala 2.13](https://img.shields.io/maven-central/v/software.purpledragon/sttp-scribe_2.13.svg?label=scala%202.13)](https://central.sonatype.com/search?q=a:sttp-scribe_2.13)

A backend implementation for [sttp](https://github.com/softwaremill/sttp) that allows you to use
[ScribeJava](https://github.com/scribejava/scribejava) as a backend. Now you can call OAuth endpoints using sttp!

## Quickstart

Add sttp-scribe, sttp and Scribe to your project:

```scala
libraryDependencies ++= Seq(
  "software.purpledragon"         %% "sttp-scribe"      % <version>,
  "com.softwaremill.sttp.client"  %% "core"             % "2.3.0",
  "com.github.scribejava"         %  "scribejava-apis"  % "8.3.3",    
)
```

Setup your Scribe service:

```scala
val service = new ServiceBuilder("api-key")
  .apiSecret("api-secret")
  .callback("http://www.example.com/oauth_callback/")
  .build(GitHubApi.instance())
```

Create a token provider:

```scala
val tokenProvider = new OAuth2TokenProvider() {
  private var currentToken: Option[OAuth2AccessToken] = None

  override def accessTokenForRequest: OAuth2AccessToken = {
    currentToken.getOrElse { 
      // fetch from DB or another source
    }
  }

  override def tokenRenewed(newToken: OAuth2AccessToken): Unit = {
    currentToken = Some(newToken)
    // persist token to DB
  }
}
```

Then create a `ScribeOAuth20Backend` and use sttp to make authenticated calls:

```scala
implicit val backend: SttpBackend[Identity, Nothing, NothingT] = 
  new ScribeOAuth20Backend(service, tokenProvider)

val response = basicRequest
  .get(uri"https://api.github.com/users/octocat")
  .send()
```

The Scribe backend will take care of refreshing the access token when it expires and retrying the current API call.