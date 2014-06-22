/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.sbt_play_messages

import PlayMessagesPlugin.PlayMessagesKeys
import sbt._

/**
 * PlayMessages
 */
object PlayMessages {

	val MESSAGES_FILENAME = "messages"

	val APPLICATION_LANGS_CONFIGURATION_KEY = "application.langs"

	case class Message(key: String, pattern: String)

	private def listFilesRecursively(folder: File): Array[File] = {
		val (subFolders, files) = IO.listFiles(folder).partition(_.isDirectory)
		files ++ subFolders.flatMap(listFilesRecursively)
	}

	val checkAndGenerateTask: Def.Initialize[Task[Seq[File]]] = Def.task {
		val log = Keys.streams.value.log

		val (generatedObjectPackage, generatedObjectName): (String, String) = {
			log.debug("Parsing 'PlayMessagesKeys.generatedObject'.")
			val generatedObject = PlayMessagesKeys.generatedObject.value
			if (generatedObject == null || generatedObject.isEmpty || !generatedObject.contains(".")) {
				log.error("'generatedObject' must contain an object package and name.")
				(null, null)
			} else {
				val dotLastIndex = generatedObject.lastIndexOf(".")
				val _package = generatedObject.take(dotLastIndex)
				val name = generatedObject.drop(dotLastIndex + 1)
				log.debug("_package [" + _package + "]; name [" + name + "].")
				(_package, name)
			}
		}

		val messagesFiles: Array[File] = {
			log.debug("Loading messages files.")
			val confDirectory = play.PlayImport.PlayKeys.confDirectory.value
			val messagesFiles = IO.listFiles(confDirectory).filter(_.getName.startsWith(MESSAGES_FILENAME)).sortBy(_.getName)
			if (messagesFiles.nonEmpty && PlayMessagesKeys.requireDefaultMessagesFile.value && messagesFiles.head.getName != MESSAGES_FILENAME) {
				log.error("The default messages file must exist. [" + confDirectory + java.io.File.separator + MESSAGES_FILENAME + "]")
				Array.empty
			} else {
				log.debug("messagesFiles [" + messagesFiles + "].")
				messagesFiles
			}
		}

		val defaultMessages: Seq[Message] = {
			if (messagesFiles.isEmpty) Nil
			else {
				log.info("Loading default messages.")
				play.api.i18n.PlayMessagesMessageParser.parse(messagesFiles.head) match {
					case Left(ex) =>
						log.error("Exception parsing the default messages file [" + messagesFiles.head + "].")
						Nil
					case Right(messages) =>
						log.debug("Loaded [" + messages.length + "] default messages.")
						messages
				}
			}
		}

		var requiresKeyConsistency = false // If there is no default messages file 

		if (PlayMessagesKeys.checkApplicationLanguages.value) {
			log.info("Checking 'application.langs' configuration.")
			// TODO: This does not appear to handle sbt options: -Dconfig.resource (relative in classpath); -Dconfig.file (absolute path); -Dconfig.url (URL); -Dapplication.langs=something
			play.api.Configuration.load(Keys.baseDirectory.value).getString(APPLICATION_LANGS_CONFIGURATION_KEY) match {
				case None =>
					if (messagesFiles.nonEmpty) log.error("The 'application.langs' configuration could not be detected, but messages files were.")
				case Some(setting) =>
					val applicationLangs = setting.split(",").map(_.trim).toBuffer
					val hasDefaultMessagesFile = messagesFiles.head.getName == MESSAGES_FILENAME
					(if (hasDefaultMessagesFile) messagesFiles.tail else messagesFiles).foreach { messagesFile =>
						val fileLanguage = messagesFile.getName.substring(MESSAGES_FILENAME.length + 1) // + 1 for the '.'
						if (applicationLangs.contains(fileLanguage)) applicationLangs -= fileLanguage
						else log.warn("Messages file [" + messagesFile.getName + "] language is not listed in the 'application.langs' configuration.")
					}
					if (applicationLangs.isEmpty) {
						if (hasDefaultMessagesFile) {
							log.debug("The 'application.langs' configuration languages all match language specific messages files, and you have a default messages file.")
						} else {
							log.debug("The 'application.langs' configuration languages all match language specific messages files. You do not have a default messages file. Key consistency is required.")
							requiresKeyConsistency
						}
					} else if (applicationLangs.length == 1 && hasDefaultMessagesFile) {
						log.debug("The 'application.langs' configuration languages match the language specific messages files or the default messages file.")
					} else {
						log.error("The 'application.langs' configuration has languages [" + applicationLangs.mkString(";") + "] missing messages files.")
					}
			}
		}

		val defaultMessageKeysDistinctSorted = defaultMessages.map(_.key).distinct.sorted

		if (messagesFiles.nonEmpty) {
			val languageSpecificMessages: Array[(Seq[Message], File)] = {
				if (messagesFiles.length < 2) Array.empty
				else {
					log.info("Loading non-default messages.")
					messagesFiles.tail.map { messagesFile =>
						play.api.i18n.PlayMessagesMessageParser.parse(messagesFile) match {
							case Left(ex) =>
								log.error("Exception parsing a messages file [" + messagesFile + "].")
								(Nil, messagesFile)
							case Right(messages) =>
								log.debug("Loaded [" + messages.length + "] messages from [" + messagesFile + "].")
								(messages, messagesFile)
						}
					}
				}
			}

			if (PlayMessagesKeys.checkDuplicateKeys.value) {
				log.info("Checking for duplicate keys.")
				((defaultMessages, messagesFiles.head) +: languageSpecificMessages).foreach {
					case (messages: Seq[Message], file: File) =>
						val duplicateMessageKeys = messages.groupBy(_.key).filter(_._2.length > 1).keys
						if (duplicateMessageKeys.nonEmpty) log.warn("Messages file [" + file + "] contains duplicate keys [" + duplicateMessageKeys.mkString("; ") + "].")
						else log.debug("Messages file [" + file + "] contains no duplicate keys.")
				}
			}

			if (PlayMessagesKeys.checkKeyConsistency.value) {
				log.info("Checking key consistency.")
				val skipFilenames = PlayMessagesKeys.checkKeyConsistencySkipFilenames.value
				val nonexistentFilenames = skipFilenames -- messagesFiles.map(_.getName)
				if (nonexistentFilenames.nonEmpty) log.warn("'checkKeyConsistencySkipFilenames' contains filenames that do not exist.")
				languageSpecificMessages.filterNot(tmp => skipFilenames.contains(tmp._2.getName)).foreach {
					case (messages: Seq[Message], file: File) =>
						val currentMessageKeys = messages.map(_.key).distinct
						val missingKeys = defaultMessageKeysDistinctSorted.diff(currentMessageKeys)
						if (missingKeys.nonEmpty) {
							val msg = "Messages file [" + file + "] is missing some keys. [" + missingKeys.mkString("; ") + "]"
							if (requiresKeyConsistency) log.error(msg)
							else log.warn(msg)
						}
						val extraKeys = currentMessageKeys.diff(defaultMessageKeysDistinctSorted)
						if (extraKeys.nonEmpty) log.warn("Messages file [" + file + "] contains keys [" + extraKeys.mkString("; ") + "] not in the default messages file.")
						if (missingKeys.isEmpty && extraKeys.isEmpty) log.debug("Messages file [" + file + "] is ok.")
				}
			}

			if (PlayMessagesKeys.checkKeysUsed.value) {
				if (!PlayMessagesKeys.generateObject.value) log.warn("Not checking key usage as object generation is disable.")
				else if (generatedObjectName == null) log.warn("Unable to check key usage due to the 'generatedObject' error above.")
				else {
					log.info("Checking key usage.")
					val ignoreFilenames = PlayMessagesKeys.checkKeysUsedIgnoreFilenames.value
					val ignoreKeys = PlayMessagesKeys.checkKeysUsedIgnoreKeys.value
					val messagesReferenceRegex = s"""[\\s\\.,(]$generatedObjectName\\.([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)""".r
					val sourceFiles = (Keys.unmanagedSourceDirectories in Compile).value.distinct.flatMap(listFilesRecursively).filterNot(file => ignoreFilenames.exists(file.getAbsolutePath.endsWith(_)))
					val referencedMessageKeys = sourceFiles.flatMap { file =>
						messagesReferenceRegex.findAllMatchIn(IO.readLines(file).mkString).map(_.group(1)).toSeq
					}.distinct
					val remainingMessageKeys = defaultMessageKeysDistinctSorted.filterNot(messageKey => ignoreKeys.exists(messageKey.matches(_))).diff(referencedMessageKeys)
					if (remainingMessageKeys.nonEmpty) log.warn("Some message keys [" + remainingMessageKeys.mkString("; ") + "] are not used.")
					else log.debug("All messages keys that are not to be ignored are used.")
				}
			}
		}

		val generateScala = PlayMessagesKeys.generateScala.value

		val generatedObjectFile = (Keys.sourceManaged in Compile).value / (PlayMessagesKeys.generatedObject.value.replace('.', '/') + (if (generateScala) ".scala" else ".java"))

		if (!PlayMessagesKeys.generateObject.value || defaultMessages.isEmpty) {
			if (generatedObjectFile.exists) {
				IO.delete(generatedObjectFile)
				log.debug("Deleted existing generatedObjectFile [" + generatedObjectFile + "].")
			}
			Nil
		} else {
			log.info("Generating object.")
			val currentObjectNesting = scala.collection.mutable.ArrayBuffer.empty[String]
			val objectContent = if (generateScala) "" + // Scala Output
				// Open root object and generated classes.
				s"""package $generatedObjectPackage
				   |
				   |import com.github.evanbennett.play_messages_scala._
				   |
				   |/** Play Message Keys Object generated by PlayMessagesPlugin. */
				   |object $generatedObjectName extends RootMessageObject {
				   |
				   |""".stripMargin +
				// Output message values.
				defaultMessageKeysDistinctSorted.map { messageKey =>
					val messageKeyParts = messageKey.split('.')
					var matchesSoFar = true
					"" +
						// Close any open objects that are not required.
						(for (((currentNesting, requiredNesting), i) <- currentObjectNesting.zipAll(messageKeyParts.dropRight(1), null, null).zipWithIndex if currentNesting != null && (!matchesSoFar || currentNesting != requiredNesting)) yield {
							if (matchesSoFar) matchesSoFar = false
							currentObjectNesting -= currentNesting
							("	" * (i + 1)) + "}\n"
						}).reverse.mkString +
						// Open any objects that are required and not open.
						(for (((currentNesting, requiredNesting), i) <- currentObjectNesting.zipAll(messageKeyParts.dropRight(1), null, null).zipWithIndex if currentNesting == null) yield {
							currentObjectNesting += requiredNesting
							("	" * (i + 1)) + s"""object $requiredNesting extends MessageKeyPart("${messageKeyParts.take(i + 1).mkString(".")}.") {""" + "\n"
						}).mkString +
						// Output message value.
						("	" * (currentObjectNesting.length + 1)) + s"""val ${messageKeyParts.last} = Message("$messageKey")""" + "\n"
				}.mkString +
				// Close any open objects.
				(for (i <- currentObjectNesting.length until 0 by -1) yield ("	" * i) + "}\n").mkString +
				// Close root object.
				"}"
			else "" + // Java Output
				// Open root object and generated classes.
				s"""package $generatedObjectPackage;
				   |
				   |import com.github.evanbennett.play_messages_java.*;
				   |
				   |/*
				   | * Play Message Keys Object generated by PlayMessagesPlugin.
				   | */
				   |public class $generatedObjectName {
				   |
				   |	private $generatedObjectName() {}
				   |
				   |	public static String get(String key, Object... args) {
				   |		return play.i18n.Messages.get(key, args);
				   |	}
				   |
				   |	public static String get(play.api.i18n.Lang lang, String key, Object... args) {
				   |		return play.i18n.Messages.get(lang, key, args);
				   |	}
				   |
				   |""".stripMargin +
				// Output message values.
				defaultMessageKeysDistinctSorted.map { messageKey =>
					val messageKeyParts = messageKey.split('.')
					var matchesSoFar = true
					"" +
						// Close any open objects that are not required.
						(for (((currentNesting, requiredNesting), i) <- currentObjectNesting.zipAll(messageKeyParts.dropRight(1), null, null).zipWithIndex if currentNesting != null && (!matchesSoFar || currentNesting != requiredNesting)) yield {
							if (matchesSoFar) matchesSoFar = false
							currentObjectNesting -= currentNesting
							("	" * (i + 1)) + "}\n"
						}).reverse.mkString +
						// Open any objects that are required and not open.
						(for (((currentNesting, requiredNesting), i) <- currentObjectNesting.zipAll(messageKeyParts.dropRight(1), null, null).zipWithIndex if currentNesting == null) yield {
							currentObjectNesting += requiredNesting
							val j = i + 1
							"" +
								("	" * j) + s"""public static class $requiredNesting {""" + "\n" +
								("	" * j) + s"""	private $requiredNesting() {}""" + "\n" +
								("	" * j) + s"""	public static String get(String additionalKey, Object... args) {""" + "\n" +
								("	" * j) + s"""		 return play.i18n.Messages.get("${messageKeyParts.take(j).mkString(".")}." + additionalKey, args);""" + "\n" +
								("	" * j) + s"""	}""" + "\n" +
								("	" * j) + s"""	public static String get(play.api.i18n.Lang lang, String additionalKey, Object... args) {""" + "\n" +
								("	" * j) + s"""		 return play.i18n.Messages.get(lang, "${messageKeyParts.take(j).mkString(".")}." + additionalKey, args);""" + "\n" +
								("	" * j) + s"""	}""" + "\n"
						}).mkString +
						// Output message value.
						("	" * (currentObjectNesting.length + 1)) + s"""public static final Message ${messageKeyParts.last} = new Message("$messageKey");""" + "\n"
				}.mkString +
				// Close any open objects.
				(for (i <- currentObjectNesting.length until 0 by -1) yield ("	" * i) + "}\n").mkString +
				// Close root object.
				"}"

			val currentObjectContent: String = {
				if (generatedObjectFile.exists) {
					log.debug("Loading existing generatedObjectFile [" + generatedObjectFile + "].")
					IO.readLines(generatedObjectFile).mkString
				} else {
					IO.createDirectory(generatedObjectFile.getParentFile)
					log.debug("Creating new generatedObjectFile [" + generatedObjectFile + "].")
					null
				}
			}
			if (objectContent != currentObjectContent) {
				IO.write(generatedObjectFile, objectContent)
				log.debug("generatedObjectFile [" + generatedObjectFile + "] written.")
			}

			Seq(generatedObjectFile)
		}
	}
}