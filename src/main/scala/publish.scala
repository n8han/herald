package herald

import dispatch._

import scala.xml.{XML,Source}
object Publish {
  def apply(body: Seq[xml.Node],
            email: String,
            password: String,
            siteId: Int,
            title: String,
            name: String): Promise[Either[String, String]] = {
    val source = <a href="http://github.com/n8han/herald">herald</a>
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
    }
  }
  def httperror(t: Throwable) =
    "Error communicating with Posterous: " + t.getMessage

  def asXml = As.string.andThen { str => XML.load(Source.fromString(str)) }

  def asStatus = new FunctionHandler( _.getStatusCode )

  /** Check that the current version's notes aren't already posted */
  def duplicate(email: String, pass: String, site: String, title: String)
  : Promise[Option[String]]= {
    val posting = :/(site) / title.replace(" ", "-").replace(".", "")
    Http(posting.HEAD > asStatus).either.map {
      _.fold(
        err => Some(httperror(err)),
        code => code match {
          case 200 | 302 =>
            Some("Someone has already published %s !\n-> %s".format(
              title, posting.url
            ))
          case _ => None
        }
      )
    }
  }

  def posterousApi(email: String, password:String) =
    host("posterous.com").secure / "api" as (email, password)
}
