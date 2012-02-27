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
      ) > As.string).either
    ) yield {
      eth.left.map { exc =>
        "Error posting to Posterous site %d: %s".format(
          siteId, exc.getMessage
        )
      }.right.flatMap { str =>
        (XML.load(Source.fromString(str)) \ "post" \ "url").map {
          nd => Right(nd.text)
        }.headOption.getOrElse {
          Left("No post URL found in response: " + str)
        }
      }
    }
  }


/*
  private def checkTask =
    (email, password, siteId, streams) map { 
      (emailOpt, passOpt, siteId, s) =>
        val pass = require(passOpt, password)
        val em = require(emailOpt, email)
        http { _(posterousApi(em, pass) / "getsites" <> { rsp =>
          s.log.info("%s contributes to the following sites:" format
                     email)
          for {
            site <- rsp \ "site"
            id <- site \ "id"
            name <- site \ "name"
          } s.log.info("  %-10s%s" format (id.text, name.text))

          rsp \ "site" \ "id" filter {
            _.text == siteId.toString
          } match {
            case ids if ids.isEmpty =>
              s.log.error("You are not authorized to contribute to %d, the configured posterousSiteId." format siteId)
            case _ => 
              s.log.success("You may contribute to %d, this project's postSiteId." format siteId)
          }
        }) }
  }
*/

  /** Check that the current version's notes aren't already posted */
/*  def dupCheckTask =
    (email, password, site, title,
     streams) map {     
      (email, pass, site, title, s) =>
        val posting = :/(site) / title.replace(" ", "-").replace(".", "")
        http { _.x(Handler(posting.HEAD, { 
          case (200 | 302, _, _) =>
            sys.error("Someone has already posted notes for %s as %s" format
                  (title, posting.to_uri)) 
          case _ => ()
        }: Handler.F[Unit])) }
    }
*/

  def posterousApi(email: String, password:String) =
    host("posterous.com").secure / "api" as (email, password)
}
