package herald

import dispatch._
import com.ning.http.client.FluentStringsMap
import com.ning.http.client.oauth._

trait AnHttp {
  def http: Executor
}

trait AConsumer {
  def consumer: ConsumerKey
}

trait OAuthEndpoints {
  def requestToken: String
  def accessToken: String
  def authorize: String
}

trait ACallback {
  def callback: String
}

trait TumblrEndpoints extends OAuthEndpoints {
  val requestToken = "http://www.tumblr.com/oauth/request_token"
  val accessToken  = "http://www.tumblr.com/oauth/access_token"
  val authorize    = "http://www.tumblr.com/oauth/authorize"
}

object HeraldAuth extends OAuth
  with HeraldConsumer
  with ACallback 
  with TumblrEndpoints
  with AnHttp {
  val http = dispatch.Http
  val callback = "http://127.0.0.1:8888/"
}

trait OAuth {
  self: AnHttp
    with AConsumer 
    with ACallback
    with OAuthEndpoints =>
  private val random = new java.util.Random(System.identityHashCode(this) +
                                            System.currentTimeMillis)
  private val nonceBuffer = Array.fill[Byte](16)(0)
  val emptyToken = new RequestToken("", "")

  def generateNonce = nonceBuffer.synchronized {
    random.nextBytes(nonceBuffer)
    com.ning.http.util.Base64.encode(nonceBuffer)
  }

  def message[A](promised: Promise[A], ctx: String) =
    for (exc <- promised.either.left)
      yield "Unexpected problem fetching %s:\n%s".format(ctx, exc.getMessage)

  def fetchRequestToken: Promise[Either[String,RequestToken]]  = {
    val prepared = http.client.preparePost(requestToken)
      .addParameter("oauth_callback", callback)
      .setSignatureCalculator(
        new OAuthSignatureCalculator(consumer, emptyToken)
      )
    val promised = http(prepared.build, new OkFunctionHandler(AsOAuth.token))
    for (eth <- message(promised, "request token")) yield eth.joinRight
  }

  def signedAuthorize(reqToken: RequestToken) = {
    val calc = new OAuthSignatureCalculator(consumer, reqToken)
    val timestamp = System.currentTimeMillis() / 1000L
    val unsigned = url(authorize) <<? Map("oauth_token" -> reqToken.getKey)
    val sig = calc.calculateSignature("GET",
                                      unsigned.url,
                                      timestamp,
                                      generateNonce,
                                      new FluentStringsMap,
                                      new FluentStringsMap)
    (unsigned <<? Map("oauth_signature" -> sig)).url
  }

  def fetchAccessToken(reqToken: RequestToken, verifier: String)
  : Promise[Either[String,RequestToken]]  = {
    val prepared = http.client.preparePost(accessToken)
      .addParameter("oauth_verifier", verifier)
      .setSignatureCalculator(
        new OAuthSignatureCalculator(consumer, reqToken)
      )
    val promised = http(prepared.build, new OkFunctionHandler(AsOAuth.token))
    for (eth <- message(promised, "access token")) yield eth.joinRight
  }

}

object AsOAuth {
  def decode(str: String) = java.net.URLDecoder.decode(str, "utf-8")
  def formDecode(str: String) =
    (for (pair <- str.trim.split('&'))
      yield pair.split('=')
    ).collect {
      case Array(key, value) => decode(key) -> decode(value)
    }

  val token = As.string.andThen { str =>
    val params = formDecode(str)
    (for {
      ("oauth_token", tok) <- params
      ("oauth_token_secret", secret) <- params
    } yield new RequestToken(tok, secret)).headOption.toRight {
      "No token found in response: \n\n" + str
    }
  }
}
