name := "posterous-sbt"

organization := "net.databinder"

version := "0.3.0"

libraryDependencies ++= Seq(
                    "net.databinder" %% "dispatch-http" % "0.8.5",
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16"
)

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

sbtPlugin := true