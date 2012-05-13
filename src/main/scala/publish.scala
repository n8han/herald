package herald

import dispatch._

import scala.xml.{XML,Source}

import com.ning.http.client.oauth.RequestToken

object Publish {
  def apply(body: Seq[xml.Node],
            accessToken: RequestToken,
            siteId: Int,
            title: String,
            name: String): Promise[Either[String, String]] = {
sys.error("not defined")
/*    val source = <a href="http://github.com/n8han/herald">herald</a>
    for (eth <- 
      Http(posterousApi(email, password) / "newpost" << Map(
        "site_id" -> siteId.toString,
        "title" -> title,
        "tags" -> name,
        "body" -> body.mkString,
        "source" -> source.toString
      ) > asXml).either
    ) yield {
      eth.left.map(httperror).right.flatMap { xml =>
        (xml \ "post" \ "url").map {
          nd => Right(nd.text)
        }.headOption.getOrElse {
          Left("No post URL found in response: " + xml)
        }
      }
    }*/
  }
  def httperror(t: Throwable) =
    "Error communicating with Posterous: " + t.getMessage

  def asXml = As.string.andThen { str => XML.load(Source.fromString(str)) }

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

  def posterousApi(email: String, password:String) =
    host("posterous.com").secure / "api" as (email, password)
}
