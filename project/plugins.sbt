addSbtPlugin("net.databinder" % "conscript-plugin" % "0.3.4")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com",
  "sonatype" at "https://oss.sonatype.org/service/local/repositories/releases/content/"
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
