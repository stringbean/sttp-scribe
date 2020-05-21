name := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion := "2.12.11"
crossScalaVersions := Seq(scalaVersion.value, "2.11.12", "2.13.2")

// format: off
libraryDependencies ++= Seq(
  "org.slf4j"                     %  "slf4j-api"        % "1.7.26",
  "com.softwaremill.sttp.client"  %% "core"             % "2.1.2",
  "com.github.scribejava"         %  "scribejava-apis"  % "6.9.0",
  "com.github.bigwheel"           %% "util-backports"   % "2.1",
  "org.scalatest"                 %% "scalatest"        % "3.0.8"   % Test
)
// format: on

headerLicense := Some(HeaderLicense.ALv2("2018", "Michael Stringer"))
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
developers := List(
  Developer("stringbean", "Michael Stringer", "@the_stringbean", url("https://github.com/stringbean"))
)
organizationName := "Purple Dragon Software"
organizationHomepage := Some(url("https://purpledragon.software"))
homepage := Some(url("https://github.com/stringbean/sttp-scribe"))
scmInfo := Some(
  ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git"))

bintrayPackageLabels := Seq("sttp", "scribe", "oauth")

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
