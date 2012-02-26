seq(conscriptSettings :_*)

name := "herald-app"

organization := "net.databinder.herald"

version := "0.4.0-SNAPSHOT"

libraryDependencies ++= Seq(
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16",
                    "net.databinder" %% "unfiltered-netty-server" % "0.6.0",
                    "net.databinder.dispatch" %% "core" % "0.9.0-alpha2"
)

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")