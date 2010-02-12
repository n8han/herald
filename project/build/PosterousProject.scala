import sbt._

class PosterousProject(info: ProjectInfo) extends PluginProject(info) {
  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.0-beta2"
}