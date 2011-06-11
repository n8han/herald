package posterous

import sbt._
import Keys._
import Defaults._
import Project.Initialize

import dispatch._
import java.net.URI
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.{NodeSeq,Node}

object PublishPlugin extends Plugin {
  val posterousEmail = SettingKey[String]("posterous-email")
  val posterousPassword = SettingKey[String]("posterous-password")
  /** Posterous site id, defaults to implicit.ly */
  val posterousSiteId = SettingKey[Int]("posterous-site-id")
  /** Hostname of target site, used to check that a post is not a duplicate */
  val posterousSite = SettingKey[String]("posterous-site")

  val posterousTags = SettingKey[Seq[String]]("posterous-tags")

  /** Title defaults to name and version */
  val posterousTitle = SettingKey[String]("posterous-title")
  /** Path to release notes and text about project. */
  val posterousNotesDirectory =
    SettingKey[File]("posterous-notes-directory")
  val posterousNotes = SettingKey[File]("posterous-notes")
  val posterousNotesVersion = SettingKey[String]("notes-version")
  /** Project info named about.markdown. */
  val posterousAbout = SettingKey[File]("posterous-about")
  private def notesExtension = ".markdown"

  val posterousBody = TaskKey[NodeSeq]("posterous-body")
  val publishNotes = TaskKey[Unit]("publish-notes")
  val previewNotes = TaskKey[Unit]("preview-notes")

  override val settings: Seq[Project.Setting[_]] = Seq(
    posterousSiteId := 1031779,
    posterousSite := "implicit.ly",
    posterousTags <<= (crossScalaVersions, name, organization) {
      (csv, n, o) => csv.map { "Scala " + _ } :+ n :+ o
    },
    posterousNotesVersion <<= (version) { v =>
      "-SNAPSHOT$".r.replaceFirstIn(v, "")
    },
    posterousTitle <<= (name, posterousNotesVersion) {
      "%s %s".format(_, _) 
    },
    posterousNotesDirectory <<= baseDirectory / "notes",
    posterousNotes <<=
      (posterousNotesDirectory, posterousNotesVersion) { (pnd, pnv) =>
        pnd / (pnv + notesExtension)
    },
    posterousAbout <<=
      posterousNotesDirectory / ("about" + notesExtension),
    posterousBody <<= posterousBodyTask,
    publishNotes <<= publishNotesTask,
    previewNotes <<= previewNotesTask
  )
  /** The content to be posted, transformed into xml. Default impl
   *  is the version notes followed by the "about" boilerplate in a
   *  div of class "about" */
  private def posterousBodyTask: Initialize[Task[NodeSeq]] =
    (posterousNotes, posterousAbout) map { (notes, about) =>
      mdToXml(notes) ++
        <div class="about"> { mdToXml(about) } </div>
    }

  private def publishNotesTask =
    (posterousBody, posterousEmail, posterousPassword,
     posterousSiteId, posterousTitle, posterousTags, streams) map {
      (body, email, pass, siteId, title, tags, s) =>
        val newpost = posterousApi(email, pass) / "newpost" << Map(
          "site_id" -> siteId,
          "title" -> title,
          "tags" -> tags.map { _.replace(",","_") }.toSet.mkString(","),
          "body" -> body.mkString,
          "source" -> postSource
        )
        http { _(newpost <> { rsp =>
          (rsp \ "post" \ "url").firstOption match {
            case None => Some("No post URL found in response:\n" + rsp.mkString)
            case Some(url) =>
              s.log.info("Posted release notes: " + url.text)
              tryBrowse(new URI(url.text), None)
              None
          }
        }) }
        ()
    }

  private def previewNotesTask = 
    (posterousBody, posterousTitle, posterousTags, target, streams) map {
      (body, title, tags, out, s) =>
        val notesOutput = out / "posterous-preview.html"
        IO.write(notesOutput, 
            <html>
            <head>
              <title> { title } </title>
              <style> {"""
                div.about * { font-style: italic }
                div.about em { font-style: normal }
              """} </style>
            </head>
            <body>
              <h2><a href="#">{ title }</a></h2>
              { body }
              <div id="tags">
                { mdToXml(tags.map("[%s](#)" format _) mkString("""Filed under // """, " ", "")) }
              </div>
            </body>
            </html> mkString
        )
        s.log.info("Saved release notes: " + notesOutput)
        tryBrowse(notesOutput.toURI, Some(s))
    }

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

  /** Opens uri in a browser if on a JVM 1.6+ */
  private def tryBrowse(uri: URI, s: Option[TaskStreams]){
    try {
      val dsk = Class.forName("java.awt.Desktop")
      dsk.getMethod("browse", classOf[java.net.URI]).invoke(
        dsk.getMethod("getDesktop").invoke(null), uri
      )
    } catch { case e => 
      s.map { _.log.error(
        "Error trying to preview notes:\n\t" + rootCause(e).toString) 
      }
    }
  }
  private def rootCause(e: Throwable): Throwable =
    if(e.getCause eq null) e else rootCause(e.getCause)
    
  private def posterousApi(email: String, password:String) =
    :/("posterous.com").secure / "api" as_! (email, password)

  def http(block: Http => Option[String]) = try {
    block(new Http)
  } catch {
    case e: StatusCode => Some(e.getMessage)
  }
  
/*
  /** @returns Some(error) if a note publishing requirement is not met */
  def publishNotesReqs(vers: String) = localNotesReqs(vers) orElse 
    credentialReqs orElse uniquePostReq(vers)
  def credentialReqs = ( missing(posterousCredentialsPath, "credentials file")
    ) orElse { missing(posterousEmail, posterousCredentialsPath, "email")
    } orElse { missing(posterousPassword, posterousCredentialsPath, "password") }
  def localNotesReqs(version: String) = missing(versionNotesPath(version), "release notes file")
  

  /** Check that the current version's notes aren't already posted to posterous */
  def uniquePostReq(vers: String) = {
    val posting = :/(posterousSite) / postTitle(vers).replace(" ", "-").replace(".", "")
    http { _.x(posting.HEAD) { 
      case (200 | 302, _, _) =>  Some("Someone has already posted notes on version %s at %s" format(
        vers, posting.to_uri
      )) 
      case _ => None
    } }
  }

  lazy val checkPosterous = checkPosterousAction
  def checkPosterousAction = task { credentialReqs orElse {
    http { _(posterousApi / "getsites" <> { rsp =>
      log.info("%s contributes to the following sites:" format posterousEmail)
      for (site <- rsp \ "site"; id <- site \ "id"; name <- site \ "name")
        log.info("  %-10s%s" format (id.text, name.text))
      
      rsp \ "site" \ "id" filter { _.text == postSiteId.toString } match {
        case ids if ids.isEmpty => Some("You are not authorized to contribute to %d, this project's postSiteId." format postSiteId)
        case _ => 
          log.success("You may contribute to %d, this project's postSiteId." format postSiteId)
          None
      }
    }) }
  } } describedAs ("Check Posterous credentials and permissions for the current postSiteId.")


 def missing(path: Path, title: String) =
    Some(path) filter (!_.exists) map { ne =>
      "Missing %s, expected in %s" format (title, path)
    }

  def missing(str: String, path: Path, title: String) = 
    Some(str) filter { _ == "" } map { str =>
      "Missing value %s in %s" format (title, path)
    }
    
*/
}

