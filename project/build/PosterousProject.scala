import sbt._

class PosterousProject(info: ProjectInfo) extends PluginProject(info) {
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.0-beta2"

  val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
  val knockoff = "com.tristanhunt" %% "knockoff" % "0.6.1-8"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}