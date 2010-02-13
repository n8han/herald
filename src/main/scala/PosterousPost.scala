package posterous
import sbt._

import dispatch._
import java.net.URI

trait Post extends BasicDependencyProject {
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
  def postBody = "release notes <i>stuff</i>"
  def postSource = "<a href='http://github.com/n8han/posterous-sbt'>posterous-sbt plugin</a>"

  lazy val publishNotes = publishNotesAction
  def publishNotesAction = task {
    val api = :/("posterous.com").secure / "api" as_! (posterousUsername, posterousPassword)
    val post = api / "newpost" << Map(
        "site_id" -> postSiteId,
        "title" -> postTitle,
        "tags" -> postTags.map { _.replace(",","_") }.removeDuplicates.mkString(","),
        "body" -> postBody,
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
  def tryBrowse(uri: URI) { 
    try {
      val dsk = Class.forName("java.awt.Desktop")
      dsk.getMethod("browse", classOf[java.net.URI]).invoke(
        dsk.getMethod("getDesktop").invoke(null), uri
      )
    } catch { case _ => () }
  }
}