package herald

import dispatch._
import java.net.URI
import java.io.{File,FileInputStream}
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.{NodeSeq,Node}

object Herald {
  def posterousCredentialsPath = 
    file(new File(System.getProperty("user.home")), ".posterous")

  lazy val posterousProperties =
    posterousCredentialsPath.right.map { file =>
      val props = new java.util.Properties
      val is = new FileInputStream(file)
      props.load(is)
      is.close()
      props
    }

  def posterousProperty(name: String) =
    posterousProperties.right.flatMap { p =>
      Option(p.getProperty(name)).toRight {
        "Required property %s not found in file %s".format(
          name, posterousCredentialsPath
        )
      }
    }

  def posterousEmail = posterousProperty("email")
  def posterousPassword = posterousProperty("password")

  def file(parent: File, child: String) =
    Some(new File(parent, child)).filter { _.exists }.toRight {
      "Required path %s/%s does not exist".format(parent.toString, child)
    }

  /** Posterous site id, defaults to implicit.ly */
  def siteId = 1031779
  def site = "implicit.ly"

  def base = new File(".").getCanonicalFile

  /** Title defaults to name and version */
  def title = version.right.map { v => "%s %s".format(name, v) }
  def name = base.getName

  /** Path to release notes and text about project. */
  def notesDirectory = file(base, "notes")

  def version: Either[String,String] = Left("version is todo")
  def notesFile: Either[String, File] = for {
    notes <- notesDirectory.right
    v <- version.right
    f <- file(notes, v + notesExtension).right
  } yield f

  /** Project info named about.markdown. */
  def aboutFile = notesDirectory.right.flatMap { n =>
    file(n, "about" + notesExtension)
  }
  private def notesExtension = ".markdown"

  /** The content to be posted, transformed into xml. Default impl
   *  is the version notes followed by the "about" boilerplate in a
   *  div of class "about" */
  def bodyContent =
    for {
      notes <- notesFile.right
      about <- aboutFile.right
    } yield
      mdToXml(notes) ++
        <div class="about"> { mdToXml(about) } </div>

/*  def publishNotes =
    (body, email, password, siteId,
     title, tags, dupCheck, streams) map {
      (body, emailOpt, passOpt, siteId, title, tags, _, s) =>
        val pass = require(passOpt, password)
        val em = require(emailOpt, email)
        val newpost = posterousApi(em, pass) / "newpost" << Map(
          "site_id" -> siteId.toString,
          "title" -> title,
          "tags" -> tags.map { _.replace(",","_") }.toSet.mkString(","),
          "body" -> body.mkString,
          "source" -> postSource.toString
        )
        http { _(newpost <> { rsp =>
          (rsp \ "post" \ "url").headOption match {
            case None => Some("No post URL found in response:\n" + rsp.mkString)
            case Some(url) =>
              s.log.success("Posted release notes: " + url.text)
              tryBrowse(new URI(url.text), None)
              None
          }
        }) }
    }
*/

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
  /** @return node sequence from str or Nil if str is null or empty. */
  private def mdToXml(str: String) = str match {
    case null | "" => Nil
    case _ => toXML(knockoff(str))
  }   

  /** @return node sequence from file or Nil if file is not found. */
  private def mdToXml(md: File) =
    if (md.exists)
      toXML(knockoff(scala.io.Source.fromFile(md).mkString))
    else
      Nil

  /** Agent that is posting to Posterous (this plugin) */
  private def postSource =
    <a href="http://github.com/n8han/posterous-sbt">posterous-sbt plugin</a>

/*  private def posterousApi(email: String, password:String) =
    :/("posterous.com").secure / "api" as_! (email, password)

  def http[T](block: Http => T) {
    val h = new Http
    try { block(h) }
    finally { h.shutdown }
  }
*/
  def run(args: Array[String]) = {
    (for {
      _ <- bodyContent.right
      _ <- title.right
    } yield {
      Preview(bodyContent, title)
      "Stopped preview."
    }).fold(
      err => { System.err.println(err); 1 },
      msg => { println(msg); 0 }
    )
  }
  def main(args: Array[String]) {
    System.exit(run(args))
  }
}

class Herald extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) = {
    Exit(Herald.run(config.arguments))
  }
  case class Exit(val code: Int) extends xsbti.Exit
}
