name := "posterous-sbt"

organization := "net.databinder"

version := "0.2.1-SNAPSHOT"

scalaVersion := "2.8.1"

libraryDependencies ++= Seq(
                    "net.databinder" %% "dispatch-http" % "0.8.3",
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16"
)

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

sbtPlugin := true