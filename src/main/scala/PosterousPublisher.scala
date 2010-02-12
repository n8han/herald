package posterous
import sbt._

import dispatch._
import Http._

class PosterousPublisher(info: ProjectInfo) extends PluginProject(info) {

  def posterousCredentials = Path.userHome / ".posterous"
  private lazy val posterousProperties = {
    val props = new java.util.Properties
    props.load(new java.io.FileInputStream(posterousCredentials.asFile))
    props
  }
  def posterousUsername = posterousProperties.getProperty("username")
  def posterousPassword = posterousProperties.getProperty("password")

  lazy val publishNotes = publishNotesAction
  def publishNotesAction = task {
    val api = :/("posterous.com").secure / "api" as_! (posterousUsername, posterousPassword)
    val post = api / "newpost" << Map(
        "site_id" -> 1031779,
        "title" -> "new release",
        "body" -> "release notes <i>stuff</i>"
      )
    (new Http)(post >>> System.out)
    None
  }
}