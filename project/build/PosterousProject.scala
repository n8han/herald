import sbt._

class PosterousProject(info: ProjectInfo) extends PluginProject(info) with posterous.Publish with sxr.Publish {
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.8"
  val knockoff = "com.tristanhunt" %% "knockoff" % "0.8.0-16"
  
  override def extraTags = "knockoff" :: "dispatch" :: "sbt" :: super.extraTags
  
  override def publishAction = super.publishAction && publishCurrentNotes

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
