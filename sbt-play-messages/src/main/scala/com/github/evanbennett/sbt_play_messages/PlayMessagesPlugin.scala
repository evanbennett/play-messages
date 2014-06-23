/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.sbt_play_messages

import sbt._

object PlayMessagesPlugin {

	val BUILD_VERSION = "1.0.0-RC2" // Needs to be in-sync with 'buildVersion' in build.sbt

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
		PlayMessagesPlugin.PlayMessagesKeys.checkAndGenerateTask := PlayMessages.checkAndGenerateScalaTask.value,

		Keys.libraryDependencies += "com.github.evanbennett" %% "play-messages-scala" % PlayMessagesPlugin.BUILD_VERSION
	)
}

object PlayMessagesPluginJava extends AutoPlugin {

	// 'object autoImport' is not needed as it exists in PlayMessagesPluginScala

	override lazy val requires = play.PlayJava

	override lazy val trigger = allRequirements

	override lazy val projectSettings = PlayMessagesPlugin.projectSettings ++ Seq(
		PlayMessagesPlugin.PlayMessagesKeys.checkAndGenerateTask := PlayMessages.checkAndGenerateJavaTask.value,

		Keys.libraryDependencies += "com.github.evanbennett" %% "play-messages-java" % PlayMessagesPlugin.BUILD_VERSION
	)
}