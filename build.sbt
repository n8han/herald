seq(conscriptSettings :_*)

name := "herald-app"

organization := "net.databinder.herald"

version := "0.5.1"

homepage := Some(url("https://github.com/n8han/herald"))

description :=
  "herald is a program to tell the world about your latest software release"


libraryDependencies ++= Seq(
                    "com.tristanhunt" %% "knockoff" % "0.8.0-16",
                    "net.databinder" %% "unfiltered-netty-server" % "0.6.3",
                    "net.liftweb" %% "lift-json" % "2.4",
                    "net.databinder.dispatch" %% "core" % "0.9.0-beta1",
                    "org.streum" %% "configrity-core" % "0.10.1",
                    "org.slf4j" % "slf4j-jdk14" % "1.6.2"
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishTo :=
  Some("releases" at
       "https://oss.sonatype.org/service/local/staging/deploy/maven2")

publishArtifact in Test := false

licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt"))

pomExtra := (
  <scm>
    <url>git@github.com:n8han/herald.git</url>
    <connection>scm:git:git@github.com:n8han/herald.git</connection>
  </scm>
  <developers>
    <developer>
      <id>n8han</id>
      <name>Nathan Hamblen</name>
      <url>http://github.com/n8han</url>
    </developer>
  </developers>)


//seq(lsSettings :_*)
