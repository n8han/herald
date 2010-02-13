package posterous
import sbt._

import dispatch._
import java.net.URI
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.Node

trait Post extends BasicDependencyProject with FileTasks {
  def posterousCredentials = Path.userHome / ".posterous"
  private lazy val posterousProperties = {
    val props = new java.util.Properties
    props.load(new java.io.FileInputStream(posterousCredentials.asFile))
    props
  }
  def posterousUsername = posterousProperties.getProperty("email")
  def posterousPassword = posterousProperties.getProperty("password")
  
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

  lazy val publishNotes = publishNotesAction
  def publishNotesAction = task {
    val api = :/("posterous.com").secure / "api" as_! (posterousUsername, posterousPassword)
    val post = api / "newpost" << Map(
        "site_id" -> postSiteId,
        "title" -> postTitle,
        "tags" -> postTags.map { _.replace(",","_") }.removeDuplicates.mkString(","),
        "body" -> postBody.mkString,
        "source" -> postSource
      )
    (new Http)(post <> { rsp =>
      rsp \ "post" \ "url" foreach { url =>
        log.success("Posted release notes: " + url.text)
        tryBrowse(new URI(url.text))
      }
    })
    None
  }
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