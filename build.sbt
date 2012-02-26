seq(conscriptSettings :_*)

name := "herald-app"

organization := "net.databinder.herald"

version := "0.4.0-SNAPSHOT"

libraryDependencies ++= Seq(
                    "net.databinder" %% "dispatch-http" % "0.8.5",
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16",
                    "net.databinder" %% "unfiltered-netty-server" % "0.6.0"
)

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")