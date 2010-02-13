package posterous
import sbt._

import dispatch._
import java.net.URI
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.Node

trait Post extends BasicDependencyProject with FileTasks {
  def posterousCredentialsPath = Path.userHome / ".posterous"
  private def getPosterousProperty(name: String) = {
    val props = new java.util.Properties
    props.load(new java.io.FileInputStream(posterousCredentialsPath.asFile))
    props.getProperty(name, "")
  }
  def posterousEmail = getPosterousProperty("email")
  def posterousPassword = getPosterousProperty("password")
  
  /** Posterous site id, defaults to implicit.ly */
  def postSiteId = 1031779
  /** Strings to tag the post with, defaults to the project name, organization, and Scala build versions */
  def postTags = name :: organization :: crossScalaVersions.map { "Scala " + _ }.toList
  def postTitle = "%s %s".format(name, version)
  def notesPath = path("notes")
  override def watchPaths = super.watchPaths +++ (notesPath * "*.txt")
  def versionNotesPath = notesPath / (version + ".txt")
  def aboutNotesPath = notesPath / "about.txt"
  def postBodyTxts = versionNotesPath :: aboutNotesPath :: Nil
  def txtToXml(txt: Path) =
    if (txt.exists)
      toXML(knockoff(scala.io.Source.fromFile(txt.asFile).mkString))
    else
      Nil
  def postBody = postBodyTxts flatMap txtToXml
  def postSource = "<a href='http://github.com/n8han/posterous-sbt'>posterous-sbt plugin</a>"

  def missing(path: Path, title: String) =
    Some(path) filter (!_.exists) map { ne =>
      "Missing %s, expected in %s" format (title, path)
    }

  def missing(str: String, path: Path, title: String) = 
    Some(str) filter { _ == "" } map { str =>
      "Missing value %s in %s" format (title, path)
    }

  lazy val publishNotes = publishNotesAction
  def publishNotesAction = task {
    ( missing(versionNotesPath, "release notes file")
    ) orElse { missing(posterousCredentialsPath, "credentials file")
    } orElse { missing(posterousEmail, posterousCredentialsPath, "email")
    } orElse { missing(posterousPassword, posterousCredentialsPath, "password")
    } orElse {
      val api = :/("posterous.com").secure / "api" as_! (posterousEmail, posterousPassword)
      val post = api / "newpost" << Map(
          "site_id" -> postSiteId,
          "title" -> postTitle,
          "tags" -> postTags.map { _.replace(",","_") }.removeDuplicates.mkString(","),
          "body" -> postBody.mkString,
          "source" -> postSource
        )
      try {
        (new Http)(post <> { rsp =>
          rsp \ "post" \ "url" foreach { url =>
            log.success("Posted release notes: " + url.text)
            tryBrowse(new URI(url.text))
          }
        })
        None
      } catch {
        case e: StatusCode => Some(e.getMessage)
      }
    }
  } describedAs ("Publish project release notes to Posterous")
  def notesOutputPath = outputPath / ("%s-notes.html" format artifactBaseName)
  lazy val previewNotes = previewNotesAction
  def previewNotesAction = task {
    FileUtilities.write(notesOutputPath.asFile, log) { writer =>
      writer.write(<html> { postBody } </html> toString)
      None
    } orElse {
      log.success("Saved release notes: " + notesOutputPath)
      tryBrowse(notesOutputPath.asFile.toURI)
      None
    }
  }
  def tryBrowse(uri: URI) { 
    try {
      val dsk = Class.forName("java.awt.Desktop")
      dsk.getMethod("browse", classOf[java.net.URI]).invoke(
        dsk.getMethod("getDesktop").invoke(null), uri
      )
    } catch { case _ => () }
  }
}