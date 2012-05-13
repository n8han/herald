package herald

import dispatch._

class Auth(val callback: String) extends oauth.Exchange
  with oauth.ACallback 
  with oauth.AnHttp
  with HeraldConsumer
  with TumblrEndpoints {
  val http = dispatch.Http
}

trait TumblrEndpoints extends oauth.Endpoints {
  val requestToken = "http://www.tumblr.com/oauth/request_token"
  val accessToken  = "http://www.tumblr.com/oauth/access_token"
  val authorize    = "http://www.tumblr.com/oauth/authorize"
}
