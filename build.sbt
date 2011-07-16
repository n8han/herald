name := "posterous-sbt"

organization := "net.databinder"

version := "0.2.2"

scalaVersion := "2.8.1"

libraryDependencies ++= Seq(
                    "net.databinder" %% "dispatch-http" % "0.8.3",
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16"
)

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

sbtPlugin := true

publishArtifact in (Compile, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := true

publishArtifact in (Test, packageSrc) := false