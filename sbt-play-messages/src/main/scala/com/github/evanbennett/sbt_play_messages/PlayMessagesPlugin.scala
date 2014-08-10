/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.sbt_play_messages

import sbt._

object PlayMessagesPlugin extends AutoPlugin {

	val BUILD_VERSION = "1.1.1-SNAPSHOT" // Needs to be in-sync with 'buildVersion' in build.sbt

	sealed abstract class Status
	object Statuses {
		case object Failed extends Status
		case object Succeeded extends Status
		case object NoChange extends Status
	}

	object autoImport {

		val checkTask = TaskKey[(Status, Seq[String])]("check-messages", "Check the messages.")
		val checkAndGenerateTask = TaskKey[(Status, Seq[File])]("generate-messages-object", "Check the messages and generate the object with key references.")

		object PlayMessagesKeys {
			val requireDefaultMessagesFile = SettingKey[Boolean]("play-messages-require-default-messages-file", "Require a default messages files?")
			val checkApplicationLanguages = SettingKey[Boolean]("play-messages-check-application-languages", "Check 'application.langs' from 'application.conf' against messages files?")
			val onNoChangeLoadDefaultMessageKeys = SettingKey[Boolean]("play-messages-on-no-change-load-default-message-keys", "In 'checkTask' load (and return) the default message keys even if there is no change. (For use by SBT Plugins extending this one.)")
			val checkDuplicateKeys = SettingKey[Boolean]("play-messages-check-duplicate-keys", "Check if there are any duplicate keys?")
			val checkKeyConsistency = SettingKey[Boolean]("play-messages-check-key-consistency", "Check that all the keys match across all messages files?")
			val checkKeyConsistencySkipFilenames = SettingKey[Set[String]]("play-messages-check-key-consistency-skip-filenames", "Messages filenames to skip when checking for key consistency.")
			val checkKeysUsed = SettingKey[Boolean]("play-messages-check-keys-used", "Check that all keys are used?")
			val checkKeysUsedIgnoreKeys = SettingKey[Set[String]]("play-messages-check-keys-used-ignore-keys", "Keys to ignore when checking for use. Regular expressions may be used.")
			val checkKeysUsedIgnoreFilenames = SettingKey[Set[String]]("play-messages-check-keys-used-ignore-filenames", "Source code filenames to ignore when checking for use.")
			val generateObject = SettingKey[Boolean]("play-messages-generate-object", "Generate the object with key references?")
			val generatedObject = SettingKey[String]("play-messages-generated-object", "The object package and name to generate.")
		}
	}
	import autoImport._

	override lazy val requires = play.Play

	override lazy val trigger = allRequirements

	override lazy val projectSettings = Seq(
		PlayMessagesKeys.requireDefaultMessagesFile := true,
		PlayMessagesKeys.checkApplicationLanguages := true,
		PlayMessagesKeys.onNoChangeLoadDefaultMessageKeys := false,
		PlayMessagesKeys.checkDuplicateKeys := true,
		PlayMessagesKeys.checkKeyConsistency := true,
		PlayMessagesKeys.checkKeyConsistencySkipFilenames := Set.empty[String],
		PlayMessagesKeys.checkKeysUsed := true,
		PlayMessagesKeys.checkKeysUsedIgnoreKeys := Set.empty[String],
		PlayMessagesKeys.checkKeysUsedIgnoreFilenames := Set.empty[String],
		PlayMessagesKeys.generateObject := true,
		PlayMessagesKeys.generatedObject := "conf.Messages",

		checkTask := PlayMessages.checkMessagesTask.value,

		Keys.libraryDependencies += "com.github.evanbennett" %% "play-messages-library" % BUILD_VERSION,

		Keys.sourceGenerators in Compile <+= Def.task(checkAndGenerateTask.value._2)
	)
}

object PlayMessagesScalaPlugin extends AutoPlugin {
	import PlayMessagesPlugin.autoImport._

	override lazy val requires = play.PlayScala

	override lazy val trigger = allRequirements

	override lazy val projectSettings = Seq(checkAndGenerateTask := PlayMessages.checkAndGenerateScalaTask.value)
}

object PlayMessagesJavaPlugin extends AutoPlugin {
	import PlayMessagesPlugin.autoImport._

	override lazy val requires = play.PlayJava

	override lazy val trigger = allRequirements

	override lazy val projectSettings = Seq(checkAndGenerateTask := PlayMessages.checkAndGenerateJavaTask.value)
}