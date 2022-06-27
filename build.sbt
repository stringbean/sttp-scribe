name := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion := "2.12.16"
crossScalaVersions := Seq(scalaVersion.value, "2.11.12", "2.13.8")

libraryDependencies ++= Seq(
  "org.slf4j"                     %  "slf4j-api"                % "1.7.36",
  "com.softwaremill.sttp.client"  %% "core"                     % "2.3.0",
  "com.github.scribejava"         %  "scribejava-core"          % "8.3.1",
  "com.github.bigwheel"           %% "util-backports"           % "2.1",
  "org.scalatest"                 %% "scalatest"                % "3.2.12"  % Test,
  "org.mockito"                   %% "mockito-scala-scalatest"  % "1.17.7"  % Test,
  "org.scala-lang.modules"        %% "scala-collection-compat"  % "2.7.0"   % Test,
  "commons-io"                    %  "commons-io"               % "2.11.0"  % Test,
  "org.apache.commons"            %  "commons-lang3"            % "3.12.0"  % Test,
  "ch.qos.logback"                %  "logback-classic"          % "1.2.11"  % Test
)

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.13")) {
    Nil
  } else {
    Seq("-Ypartial-unification")
  }
}

headerLicense := Some(HeaderLicense.ALv2("2018", "Michael Stringer"))
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
developers := List(
  Developer("stringbean", "Michael Stringer", "@the_stringbean", url("https://github.com/stringbean"))
)
organizationName := "Purple Dragon Software"
organizationHomepage := Some(url("https://purpledragon.software"))
homepage := Some(url("https://github.com/stringbean/sttp-scribe"))
scmInfo := Some(
  ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git")
)

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

mimaPreviousArtifacts := Set("software.purpledragon" %% "sttp-scribe" % "2.0.1")
