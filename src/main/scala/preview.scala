package herald

import unfiltered.request._
import unfiltered.response._

import com.ning.http.client.oauth.RequestToken

object Preview {
  private var requestToken = Option.empty[RequestToken]
  def apply(body: => Either[String,Seq[xml.Node]],
            title: => Either[String,String]) {
    unfiltered.netty.Http.anylocal.plan(unfiltered.netty.cycle.Planify {
      case req @ Host(host) =>
        val auth = new Auth("http://%s%s".format(host, req.uri))
        req match {
          case GET(Path(Seg(Nil))) & Params(Verifier(verifier)) =>
            val access = requestToken.toRight {
              "Unexpected state, start over and try again"
            }.right.map { tok =>
              auth.fetchAccessToken(tok, verifier)()
            }.joinRight
            foldError(for {
              accessToken <- access.right
              props <- Herald.heraldProperties.right
              path <- Herald.heraldCredentialsPath.right
            } yield {
              props
                .set("oauth_token", accessToken.getKey)
                .set("oauth_token_secret", accessToken.getSecret)
                .save(path)
              Redirect("/")
            })
          case GET(Path(Seg(Nil))) =>
            foldError(
              for (html <- previewContent(body, title).right)
                yield Html(html)
            )
          case POST(Path(Seg(Nil))) =>
            val token = Herald.accessToken
            token.fold(
              { _ =>
                auth.fetchRequestToken().fold(
                  err => ServiceUnavailable ~> ResponseString(err),
                  tok => {
                    requestToken = Some(tok)
                    Redirect(auth.signedAuthorize(tok))
                  }
                )
              },
              { accessToken =>
                NotImplemented
              }
            )
          case _ => Pass
        }
      case _ => Pass
    }).run { server =>
      unfiltered.util.Browser.open(
        "http://127.0.0.1:%d/".format(server.port)
      )
      println("\nPreviewing notes. Press CTRL+C to stop.")
    }
    def foldError[R <: ResponseFunction[Any]](eth: Either[String,R]) =
      eth.fold(
        err => InternalServerError ~> ResponseString(err),
        identity
      )
  }

  def previewContent(bodyContent: Either[String, Seq[xml.Node]],
                     title: Either[String, String]) = 
    for {
      body <- bodyContent.right
      t <- title.right
    } yield
      <html>
      <head>
        <title> { t } </title>
        <style> {"""
          div.about * { font-style: italic }
          div.about em { font-style: normal }
        """} </style>
      </head>
      <body>
        <form action="/" method="POST"><div>{
          Herald.accessToken.fold(
            _ => <input type="submit" value="Authorize with Tumblr" />
            ,
            _ => <input type="submit" value="Publish Notes" />
          )
        }</div></form>
        <h2><a href="#">{ t }</a></h2>
        { body }
      </body>
      </html>
}
object Verifier extends Params.Extract("oauth_verifier", Params.first)
