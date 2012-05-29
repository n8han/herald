package herald

import dispatch._
import oauth._

import scala.xml.{XML,Source}

import com.ning.http.client.oauth.{RequestToken,ConsumerKey}
import net.liftweb.json._

object Publish extends HeraldConsumer {
  def allowed(accessToken: RequestToken, tumblrName: String) =
    Http(
      tumblrApi / "user" / "info" <@ (consumer, accessToken) OK LiftJson.As
    ).either.left.map {
      "Error checking user info: " + _.getMessage
    }.right.map { js =>
      (for {
        JField("response", JObject(response)) <- js
        JField("user", JObject(user)) <- response
        JField("blogs", JArray(blogs)) <- user
        JObject(blog) <- blogs
        JField("name", JString(name)) <- blog
      } yield name).contains(tumblrName)
    }

  def apply(body: Seq[xml.Node],
            accessToken: RequestToken,
            hostname: String,
            title: String,
            name: String): Promise[Either[String, String]] = {
    val source = <a href="http://github.com/n8han/herald">herald</a>

    Http(tumblrApi / "blog" / hostname / "post" << Map(
      "type" -> "text",
      "title" -> title,
      "tags" -> name,
      "body" -> body.mkString,
      "source" -> source.toString // isn't doing anything!
    ) <@ (consumer, accessToken) OK LiftJson.As).either.left.map {
      "Error posting to Tumblr: " + _.getMessage
    }.map { eth =>
      eth.right.flatMap { js =>
        (for {
          JField("response", JObject(response)) <- js
          JField("id", JInt(id)) <- response
        } yield {
          "http://%s/post/%s".format(hostname, id)
        }).headOption.toRight { "unable to find id in post response" }
      }
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

  def tumblrApi =
    host("api.tumblr.com") / "v2"
}
