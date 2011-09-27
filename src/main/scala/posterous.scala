package posterous

import sbt._
import Keys._
import Defaults._
import Project.Initialize

import dispatch._
import java.net.URI
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.{NodeSeq,Node}

object Publish extends Plugin {
  val Posterous = config("posterous")

  val email = SettingKey[Option[String]]("email")
  val password = SettingKey[Option[String]]("password")
  /** Posterous site id, defaults to implicit.ly */
  val siteId = SettingKey[Int]("site-id")
  /** Hostname of target site, used to check that a post is not a duplicate */
  val site = SettingKey[String]("site")

  val tags = SettingKey[Seq[String]]("tags")

  /** Title defaults to name and version */
  val title = SettingKey[String]("title")
  /** Path to release notes and text about project. */
  val notesDirectory =
    SettingKey[File]("notes-directory")
  val notesFile = SettingKey[File]("notes-file")
  /** Project info named about.markdown. */
  val aboutFile = SettingKey[File]("about-file")
  private def notesExtension = ".markdown"

  val body = TaskKey[NodeSeq]("body")
  val notes = TaskKey[Unit]("notes")
  val preview = TaskKey[Unit]("preview")
  val check = TaskKey[Unit]("check")
  val requiredInputs = TaskKey[Unit]("required-inputs")
  val dupCheck = TaskKey[Unit]("dup-check")

  val posterousSettings: Seq[sbt.Project.Setting[_]] =
      inConfig(Posterous)(Seq(
    siteId := 1031779,
    site := "implicit.ly",
    tags <<= (crossScalaVersions, name, organization) {
      (csv, n, o) => csv.map { "Scala " + _ } :+ n :+ o
    },
    version <<= (version) { v =>
      "-SNAPSHOT$".r.replaceFirstIn(v, "")
    },
    title <<= (name, version) {
      "%s %s".format(_, _) 
    },
    notesDirectory <<= baseDirectory / "notes",
    notesFile <<=
      (notesDirectory, version) { (pnd, pnv) =>
        pnd / (pnv + notesExtension)
    },
    aboutFile <<=
      notesDirectory / ("about" + notesExtension),
    body <<= bodyTask,
    publish <<= publishNotesTask,
    (aggregate in publish) := false,
    preview <<= previewNotesTask,
    (aggregate in preview) := false,
    check <<= checkTask,
    requiredInputs <<= requiredInputsTask,
    dupCheck <<= dupCheckTask,
    email := None,
    password := None
  ))

  /** The content to be posted, transformed into xml. Default impl
   *  is the version notes followed by the "about" boilerplate in a
   *  div of class "about" */
  private def bodyTask: Initialize[Task[NodeSeq]] =
    (notesFile, aboutFile, requiredInputs) map {
      (notes, about, _) =>
        mdToXml(notes) ++
          <div class="about"> { mdToXml(about) } </div>
    }
  private def require[T](value: Option[String],
                         setting: SettingKey[Option[String]]) =
    value.getOrElse {
      sys.error("%s setting is required".format(
        setting.key.label
      ))
    }
  private def publishNotesTask =
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

  private def previewNotesTask = 
    (body, title, tags, target, streams) map {
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
        s.log.success("Saved release notes: " + notesOutput)
        tryBrowse(notesOutput.toURI, Some(s))
    }

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

  private def requiredInputsTask =
    (aboutFile, notesFile) map { (about, notes) =>
      (about :: notes :: Nil) foreach { f =>
        if (!f.exists)
          sys.error("Required file missing: " + f)
      }
    }

  /** Check that the current version's notes aren't already posted */
  def dupCheckTask =
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

  def http[T](block: Http => T) {
    val h = new Http
    try { block(h) }
    finally { h.shutdown }
  }
}
