name         := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion       := "2.13.12"
crossScalaVersions := Seq(scalaVersion.value, "2.12.18", "2.11.12")

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client" %% "core"                    % "2.3.0",
  "com.github.scribejava"         % "scribejava-apis"         % "8.3.3",
  "org.slf4j"                     % "slf4j-api"               % "1.7.36",
  "org.scala-lang.modules"       %% "scala-collection-compat" % "2.11.0",
  "com.github.bigwheel"          %% "util-backports"          % "2.1",
  "org.scalatest"                %% "scalatest"               % "3.2.17"  % Test,
  "org.mockito"                  %% "mockito-scala-scalatest" % "1.17.30" % Test,
  "commons-io"                    % "commons-io"              % "2.15.1"  % Test,
  "org.apache.commons"            % "commons-lang3"           % "3.14.0"  % Test,
  "ch.qos.logback"                % "logback-classic"         % "1.4.14"  % Test
)

scalacOptions ++= Seq(
  // format: off
  "-encoding", "UTF-8",
  // format: on
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Ywarn-unused",
  "-P:semanticdb:synthetics:on"
) ++ {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) =>
      Seq(
        "-target:jvm-1.8",
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Xlint:unsound-match"
      )

    case Some((2, 12)) =>
      Seq(
        // format: off
        "-release", "8",
        // format: on
        "-Xsource:2.13",
        "-Ypartial-unification",
        "-Xlint:unsound-match",
        "-Wconf:origin=scala.collection.compat.*:s"
      )

    case _ =>
      Seq(
        // format: off
        "-release", "8",
        // format: on
        "-Wconf:origin=scala.collection.compat.*:s"
      )
  }
}

ThisBuild / semanticdbEnabled          := true
ThisBuild / semanticdbVersion          := scalafixSemanticdb.revision
ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)

headerLicense        := Some(HeaderLicense.ALv2("2018", "Michael Stringer"))
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
developers           := List(
  Developer("stringbean", "Michael Stringer", "@the_stringbean", url("https://github.com/stringbean"))
)
organizationName     := "Purple Dragon Software"
organizationHomepage := Some(url("https://purpledragon.software"))
homepage             := Some(url("https://github.com/stringbean/sttp-scribe"))
scmInfo              := Some(
  ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git")
)
publishTo            := sonatypePublishToBundle.value

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseCrossBuild             := true
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
