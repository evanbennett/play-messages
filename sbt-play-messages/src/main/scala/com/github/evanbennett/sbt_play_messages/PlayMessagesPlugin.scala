/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.sbt_play_messages

import sbt._

object PlayMessagesPlugin {

	object PlayMessagesKeys {
		val requireDefaultMessagesFile = SettingKey[Boolean]("play-messages-require-default-messages-file", "Require a default messages files?")
		val checkApplicationLanguages = SettingKey[Boolean]("play-messages-check-application-languages", "Check 'application.langs' from 'application.conf' against messages files?")
		val checkDuplicateKeys = SettingKey[Boolean]("play-messages-check-duplicate-keys", "Check if there are any duplicate keys?")
		val checkKeyConsistency = SettingKey[Boolean]("play-messages-check-key-consistency", "Check that all the keys match across all messages files?")
		val checkKeyConsistencySkipFilenames = SettingKey[Set[String]]("play-messages-check-key-consistency-skip-filenames", "Messages filenames to skip when checking for key consistency.")
		val checkKeysUsed = SettingKey[Boolean]("play-messages-check-keys-used", "Check that all keys are used?")
		val checkKeysUsedIgnoreKeys = SettingKey[Set[String]]("play-messages-check-keys-used-ignore-keys", "Keys to ignore when checking for use. Regular expressions may be used.")
		val checkKeysUsedIgnoreFilenames = SettingKey[Set[String]]("play-messages-check-keys-used-ignore-filenames", "Source code filenames to ignore when checking for use.")
		val generateObject = SettingKey[Boolean]("play-messages-generate-object", "Generate the object with key references?")
		val generatedObject = SettingKey[String]("play-messages-generated-object", "The object package and name to generate.")
		// TODO: A user changing the 'generateScala' setting will change the generated language, but will not change the provided 'libraryDependencies'. Work out how to remove this setting.
		val generateScala = SettingKey[Boolean]("play-messages-generate-scala", "Generate Scala code (or else Java)?")

		val checkAndGenerateTask = TaskKey[Seq[File]]("play-messages-check-and-generate-task", "Check the messages and generate the object with key references.")
	}

	val projectSettings = Seq(
		PlayMessagesKeys.requireDefaultMessagesFile := true,
		PlayMessagesKeys.checkApplicationLanguages := true,
		PlayMessagesKeys.checkDuplicateKeys := true,
		PlayMessagesKeys.checkKeyConsistency := true,
		PlayMessagesKeys.checkKeyConsistencySkipFilenames := Set.empty[String],
		PlayMessagesKeys.checkKeysUsed := true,
		PlayMessagesKeys.checkKeysUsedIgnoreKeys := Set.empty[String],
		PlayMessagesKeys.checkKeysUsedIgnoreFilenames := Set.empty[String],
		PlayMessagesKeys.generateObject := true,
		PlayMessagesKeys.generatedObject := "conf.Messages",

		PlayMessagesKeys.checkAndGenerateTask := PlayMessages.checkAndGenerateTask.value,

		Keys.sourceGenerators in Compile <+= PlayMessagesKeys.checkAndGenerateTask
	)
}

object PlayMessagesPluginScala extends AutoPlugin {

	object autoImport {
		val PlayMessagesKeys = PlayMessagesPlugin.PlayMessagesKeys
	}

	override lazy val requires = play.PlayScala

	override lazy val trigger = allRequirements

	override lazy val projectSettings = PlayMessagesPlugin.projectSettings ++ Seq(
		PlayMessagesPlugin.PlayMessagesKeys.generateScala := true,

		// TODO: Work out how to use the 'version' specified in the build.sbt. Also, update below for java libary.
		Keys.libraryDependencies += "com.github.evanbennett" %% "play-messages-scala" % "1.0.0-RC2"
	)
}

object PlayMessagesPluginJava extends AutoPlugin {

	// autoImport is not needed as it exists in PlayMessagesPluginScala

	override lazy val requires = play.PlayJava

	override lazy val trigger = allRequirements

	override lazy val projectSettings = PlayMessagesPlugin.projectSettings ++ Seq(
		PlayMessagesPlugin.PlayMessagesKeys.generateScala := false,

		Keys.libraryDependencies += "com.github.evanbennett" %% "play-messages-java" % "1.0.0-RC2"
	)
}