// code style
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")

// testing
addSbtPlugin("org.scoverage" % "sbt-scoverage"   % "2.0.9")
addSbtPlugin("com.typesafe"  % "sbt-mima-plugin" % "1.1.3")

// artifact publishing
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.2.1")
addSbtPlugin("com.github.sbt" % "sbt-release"  % "1.3.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.10.0")
