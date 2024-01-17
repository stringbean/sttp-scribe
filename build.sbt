name := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion := "2.13.12"
crossScalaVersions := Seq(scalaVersion.value, "2.12.18", "2.11.12")

// format: off
libraryDependencies ++= Seq(
  "org.slf4j"                     %  "slf4j-api"                % "1.7.26",
  "com.softwaremill.sttp.client"  %% "core"                     % "2.1.2",
  "com.github.scribejava"         %  "scribejava-apis"          % "6.9.0",
  "com.github.bigwheel"           %% "util-backports"           % "2.1",
  "org.scalatest"                 %% "scalatest"                % "3.1.2"   % Test,
  "org.scalamock"                 %% "scalamock"                % "4.4.0"   % Test,
  "org.scala-lang.modules"        %% "scala-collection-compat"  % "2.1.6"   % Test,
  "commons-io"                    %  "commons-io"               % "2.7"     % Test,
  "org.apache.commons"            %  "commons-lang3"            % "3.10"    % Test,
  "ch.qos.logback"                %  "logback-classic"          % "1.2.3"   % Test,
)
// format: on

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
  ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git"))
publishTo := sonatypePublishToBundle.value

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
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

mimaPreviousArtifacts := Set("software.purpledragon" %% "sttp-scribe" % "2.0.1")
