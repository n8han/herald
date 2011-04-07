import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val posterous = "net.databinder" % "posterous-sbt" % "0.1.7"
  val sxr_publish = "net.databinder" % "sxr-publish" % "0.1.9"
}
