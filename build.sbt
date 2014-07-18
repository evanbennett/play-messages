val buildVersion = "1.1.0-RC1" // Needs to be in-sync with 'PlayMessagesPlugin.BUILD_VERSION'

// TODO: Sort out the cross building error: Play SBT Plugin is not available for 2.11.1. This does not break anything. It only errors on the project that I would disable if I could.
crossScalaVersions := Seq("2.10.4", "2.11.1")

val commonSettings = Seq(
	organization := "com.github.evanbennett",
	licenses := Seq("BSD 3-Clause License" -> url("http://opensource.org/licenses/BSD-3-Clause")),
	scmInfo := Some(ScmInfo(url("https://github.com/evanbennett/play-messages"), "git://github.com/evanbennett/play-messages.git")),
	version := buildVersion,
	scalaVersion := "2.10.4",
	scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
	publishMavenStyle := true,
	publishTo := {
		if (isSnapshot.value) Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
		else Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
	},
	pomIncludeRepository := { _ => false },
	pomExtra :=
		<developers>
			<developer>
				<name>Evan Bennett</name>
			</developer>
		</developers>
)

lazy val root = project.in(file(".")).aggregate(sbtPlayMessages, playMessagesLibrary).settings(
	publishArtifact := false,
	publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedRepo")))
)

val sbtPlayMessages = project.in(file("sbt-play-messages")).settings(commonSettings: _*).settings(
	name := "sbt-play-messages",
	description := "An SBT plugin to check Play messages files for potential problems and generate a references object.",
	homepage := Some(url("https://github.com/evanbennett/play-messages")),
	sbtPlugin := true,
	resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
	addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0"),
	libraryDependencies += "com.typesafe.play" %% "play" % "2.3.0"
)

val playMessagesLibrary = project.in(file("play-messages-library")).settings(commonSettings: _*).settings(
	name := "play-messages-library",
	description := "A small library intended for use with the 'sbt-play-messages' SBT plugin.",
	homepage := Some(url("https://github.com/evanbennett/play-messages/play-messages-library")),
	resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
	libraryDependencies += "com.typesafe.play" %% "play" % "2.3.0"
)