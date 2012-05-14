package herald

import dispatch._
import oauth._

import scala.xml.{XML,Source}

import com.ning.http.client.oauth.{RequestToken,ConsumerKey}

object Publish extends HeraldConsumer {
  def apply(body: Seq[xml.Node],
            accessToken: RequestToken,
            tumblrHostname: String,
            title: String,
            name: String): Promise[Either[String, String]] = {
    val source = <a href="http://github.com/n8han/herald">herald</a>
    for (eth <- 
      Http(tumblrApi(tumblrHostname) / "post" << Map(
        "type" -> "text",
        "title" -> title,
        "tags" -> name,
        "body" -> body.mkString,
        "source" -> source.toString
      ) <@ (consumer, accessToken) > As.string).either
    ) yield {
      eth.left.map(httperror)
    }
  }
  def httperror(t: Throwable) =
    "Error communicating with Tumblr: " + t.getMessage

  def asStatus = new FunctionHandler( _.getStatusCode )

  /** Check that the current version's notes aren't already posted */
  def duplicate(email: String, pass: String, site: String, title: String)
  : Promise[Either[String,String]] = {
    val posting = :/(site) / title.replace(" ", "-").replace(".", "")
    Http(posting.HEAD > asStatus).either.map {
      _.fold(
        err => Left(httperror(err)),
        code => code match {
          case 200 | 302 =>
            Left("Someone has already published %s !\n-> %s".format(
              title, posting.url
            ))
          case _ => Right(title)
        }
      )
    }
  }

  def tumblrApi(baseHostname: String) =
    host("api.tumblr.com") / "v2" / "blog" / baseHostname
}
