name := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion := "2.12.7"
crossScalaVersions := Seq(scalaVersion.value, "2.11.12")

libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core"             % "1.3.9",
  "com.github.scribejava" %  "scribejava-apis"  % "6.0.0",
  "org.scalatest"         %% "scalatest"        % "3.0.5"   % Test
)

headerLicense := Some(HeaderLicense.ALv2("2018", "Michael Stringer"))
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
developers := List(
  Developer("stringbean", "Michael Stringer", "@the_stringbean", url("https://github.com/stringbean"))
)
organizationName := "Purple Dragon Software"
organizationHomepage := Some(url("https://purpledragon.software"))
homepage := Some(url("https://github.com/stringbean/sttp-scribe"))
bintrayVcsUrl := Some("https://github.com/stringbean/sttp-scribe.git")
scmInfo := Some(ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git"))

bintrayPackageLabels := Seq("sttp", "scribe", "oauth")

useGpg := true
usePgpKeyHex("B19D7A14F6F8B3BFA9FF655A5216B5A5F723A92D")
pgpSecretRing := pgpPublicRing.value