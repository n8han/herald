import sbt._

class PosterousProject(info: ProjectInfo) extends PluginProject(info) with posterous.Publish with sxr.Publish {
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.4"

  val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
  val knockoff = "com.tristanhunt" %% "knockoff" % "0.7.2-13"
  
  override def extraTags = "knockoff" :: "dispatch" :: "sbt" :: super.extraTags
  
  override def publishAction = super.publishAction && publishCurrentNotes

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}