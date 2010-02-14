package posterous
import sbt._

import dispatch._
import java.net.URI
import com.tristanhunt.knockoff.DefaultDiscounter._
import scala.xml.Node

trait Publish extends BasicDependencyProject {
  def posterousCredentialsPath = Path.userHome / ".posterous"
  private def getPosterousProperty(name: String) = {
    val props = new java.util.Properties
    FileUtilities.readStream(posterousCredentialsPath.asFile, log){ input => props.load(input); None }
    props.getProperty(name, "")
  }
  def posterousEmail = getPosterousProperty("email")
  def posterousPassword = getPosterousProperty("password")
  
  /** Posterous site id, defaults to implicit.ly */
  def postSiteId = 1031779
  /** Strings to tag the post with, defaults to the project name, organization, and Scala build versions */
  def postTags = name :: organization :: crossScalaVersions.map { "Scala " + _ }.toList
  /** Title defaults to name and version */
  def postTitle = "%s %s".format(name, version)
  /** Path to release notes and text about project. */
  def notesPath = path("notes")
  override def watchPaths = super.watchPaths +++ (notesPath * ("*" + extension))
  def extension = ".markdown"
  /** Release notes named with the version and a .markdown suffix. */
  def versionNotesPath = notesPath / (version + extension)
  /** Project info named about.markdown. */
  def aboutNotesPath: Path = "about" + extension
  /** Paths to text files to be converted to xml, concatenated, and published. */
  def postBodySources = versionNotesPath :: aboutNotesPath :: Nil
  /** @return node sequence from file or Nil if file is not found. */
  def mdToXml(md: Path) =
    if (md.exists)
      toXML(knockoff(scala.io.Source.fromFile(md.asFile).mkString))
    else
      Nil
  /** Content to post, transforms postBodySources to xml and concatenates */
  def postBody = postBodySources flatMap mdToXml
  /** Agent that is posting to Posterous (this plugin) */
  def postSource = <a href="http://github.com/n8han/posterous-sbt">posterous-sbt plugin</a>
  
  def missing(path: Path, title: String) =
    Some(path) filter (!_.exists) map { ne =>
      "Missing %s, expected in %s" format (title, path)
    }

  def missing(str: String, path: Path, title: String) = 
    Some(str) filter { _ == "" } map { str =>
      "Missing value %s in %s" format (title, path)
    }

  lazy val publishNotes = publishNotesAction
  def publishNotesAction = task { publishNotes_! } describedAs ("Publish project release notes to Posterous.")

  /** @returns Some(error) if a note publishing requirement is not met */
  def publishNotesReqs = ( localNotesReqs
    ) orElse { missing(posterousCredentialsPath, "credentials file")
    } orElse { missing(posterousEmail, posterousCredentialsPath, "email")
    } orElse { missing(posterousPassword, posterousCredentialsPath, "password") }
  def localNotesReqs = missing(versionNotesPath, "release notes file")
    
  def publishNotes_! =
    publishNotesReqs orElse {
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
            tryBrowse(new URI(url.text), true) // ignore any error if not 1.6
          }
        })
        None
      } catch {
        case e: StatusCode => Some(e.getMessage)
      }
    }

  /** Where notes are saved for previewing */
  def notesOutputPath = outputPath / ("%s-notes.html" format artifactBaseName)
  lazy val previewNotes = previewNotesAction
  def previewNotesAction = task { localNotesReqs orElse {
    FileUtilities.write(notesOutputPath.asFile, <html> { postBody } </html> toString, log) orElse {
      log.success("Saved release notes: " + notesOutputPath)
      tryBrowse(notesOutputPath.asFile.toURI, false)
    }
  } } describedAs ("Preview project release notes as HTML and check for publishing credentials.")
  
  /** Opens uri in a browser if on a JVM 1.6+ */
  def tryBrowse(uri: URI, quiet: Boolean) = {
    try {
      val dsk = Class.forName("java.awt.Desktop")
      dsk.getMethod("browse", classOf[java.net.URI]).invoke(
        dsk.getMethod("getDesktop").invoke(null), uri
      )
      None
    } catch { case e => if(quiet) None else Some("Error trying to preview notes:\n\t" + rootCause(e).toString) }
  }
  private def rootCause(e: Throwable): Throwable = if(e.getCause eq null) e else rootCause(e.getCause)
}