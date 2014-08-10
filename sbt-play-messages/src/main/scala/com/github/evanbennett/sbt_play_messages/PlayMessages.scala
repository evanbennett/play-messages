/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.sbt_play_messages

import PlayMessagesPlugin.autoImport._
import PlayMessagesPlugin.Statuses._
import sbt._

/**
 * PlayMessages
 */
object PlayMessages {

	val MESSAGES_FILENAME = "messages"
	val APPLICATION_LANGS_CONFIGURATION_KEY = "application.langs"
	val CACHE_FOLDER_NAME = "com.github.evanbennett"
	val CACHE_FILE_NAME = "playMessagesToken"

	class State(val log: Logger) {
		if (log == null) throw new IllegalArgumentException("'log' must be provided.")

		var status: PlayMessagesPlugin.Status = Succeeded
	}

	case class Message(key: String, pattern: String)

	def listFilesRecursively(folder: java.io.File): Array[java.io.File] = {
		val (subFolders, files) = IO.listFiles(folder).partition(_.isDirectory)
		files ++ subFolders.flatMap(listFilesRecursively)
	}

	private def loadMessagesFile(messagesFile: java.io.File)(implicit state: State): (Seq[Message], java.io.File) = {
		play.api.i18n.PlayMessagesMessageParser.parse(messagesFile) match {
			case Right(messages) =>
				state.log.debug("Loaded messages: [" + messagesFile + "] [" + messages.length + "]")
				(messages, messagesFile)
			case Left(_) =>
				state.log.error("Error parsing the messages file: [" + messagesFile + "]")
				state.status = Failed
				(Nil, messagesFile)
		}
	}

	private def parseGeneratedObject(generatedObject: String)(implicit state: State): (String, String) = {
		state.log.debug("Parsing 'PlayMessagesKeys.generatedObject'.")
		val dotLastIndex = if (generatedObject == null) -1 else generatedObject.lastIndexOf('.')
		if (generatedObject == null || generatedObject.isEmpty || dotLastIndex < 0) {
			state.log.error("'generatedObject' must contain an object package and name.")
			state.status = Failed
			(null, null)
		} else {
			val _package = generatedObject.substring(0, dotLastIndex)
			val name = generatedObject.substring(dotLastIndex + 1)
			state.log.debug("_package [" + _package + "] name [" + name + "]")
			(_package, name)
		}
	}

	private[sbt_play_messages] val checkMessagesTask: Def.Initialize[Task[(PlayMessagesPlugin.Status, Seq[String])]] = Def.task {
		implicit val state = new State(Keys.streams.value.log)

		val messagesFiles: Array[File] = {
			state.log.debug("Loading messages files.")
			val confDirectory = play.PlayImport.PlayKeys.confDirectory.value
			val messagesFiles = IO.listFiles(confDirectory).filter(_.getName.startsWith(MESSAGES_FILENAME)).sortBy(_.getName)
			if (messagesFiles.nonEmpty && PlayMessagesKeys.requireDefaultMessagesFile.value && messagesFiles.head.getName != MESSAGES_FILENAME) {
				state.log.error("The default messages file must exist: [" + confDirectory + java.io.File.separator + MESSAGES_FILENAME + "]")
				state.status = Failed
				Array.empty
			} else {
				state.log.debug("messagesFiles: [" + messagesFiles.mkString("; ") + "]")
				messagesFiles
			}
		}

		val hasDefaultMessagesFile = (messagesFiles.nonEmpty && messagesFiles.head.getName == MESSAGES_FILENAME)

		// TODO: This does not appear to handle sbt options: -Dconfig.resource (relative in classpath); -Dconfig.file (absolute path); -Dconfig.url (URL); -Dapplication.langs=something
		val applicationLangsValue = play.api.Configuration.load(Keys.baseDirectory.value).getString(APPLICATION_LANGS_CONFIGURATION_KEY)

		if (messagesFiles.isEmpty && applicationLangsValue.isEmpty) (state.status, Nil)
		else {
			// Check if any changes have occurred.
			val generatedObjectFile = (Keys.sourceManaged in Compile).value / (PlayMessagesKeys.generatedObject.value.replace('.', '/') + ".scala")
			val newCacheValue = applicationLangsValue.getOrElse("") + ";" + messagesFiles.map(messageFile => messageFile.getName + "-" + Hash.toHex(Hash(messageFile))).mkString("[", ";", "]")
			val myCacheFolder = Keys.streams.value.cacheDirectory / CACHE_FOLDER_NAME
			if (!myCacheFolder.exists) myCacheFolder.mkdirs
			val myCacheFile = myCacheFolder / CACHE_FILE_NAME
			if (generatedObjectFile.exists && myCacheFile.exists && IO.read(myCacheFile) == newCacheValue) {
				state.status = NoChange
				state.log.info("checkMessages: NO CHANGE")
			} else IO.write(myCacheFile, newCacheValue)

			val defaultMessages: Seq[Message] = if (state.status != NoChange || PlayMessagesKeys.onNoChangeLoadDefaultMessageKeys.value) loadMessagesFile(messagesFiles.head)._1 else Nil

			val defaultMessageKeysDistinctSorted = defaultMessages.map(_.key).distinct.sorted

			if (state.status != NoChange) {
				if (PlayMessagesKeys.checkApplicationLanguages.value) {
					state.log.info("Checking 'application.langs' configuration.")
					applicationLangsValue match {
						case None =>
							if (messagesFiles.nonEmpty) {
								state.log.error("The 'application.langs' configuration could not be detected, but messages files were found.")
								state.status = Failed
							}
						case Some(setting) =>
							val applicationLangs = setting.split(",").map(_.trim).toBuffer
							(if (hasDefaultMessagesFile) messagesFiles.tail else messagesFiles).foreach { messagesFile =>
								val fileLanguage = messagesFile.getName.substring(MESSAGES_FILENAME.length + 1) // + 1 for the '.'
								if (applicationLangs.contains(fileLanguage)) applicationLangs -= fileLanguage
								else state.log.warn("Messages file language is not listed in the 'application.langs' configuration: [" + messagesFile.getName + "]")
							}
							if (applicationLangs.isEmpty) {
								if (hasDefaultMessagesFile) {
									state.log.debug("The 'application.langs' configuration languages all match language specific messages files, and you have a default messages file.")
								} else {
									state.log.debug("The 'application.langs' configuration languages all match language specific messages files. You do not have a default messages file. Key consistency is required.")
								}
							} else if (applicationLangs.length == 1 && hasDefaultMessagesFile) {
								state.log.debug("The 'application.langs' configuration languages match the language specific messages files or the default messages file.")
							} else {
								state.log.error("The 'application.langs' configuration has languages missing messages files: [" + applicationLangs.mkString("; ") + "]")
								state.status = Failed
							}
					}
				}

				val languageSpecificMessages: Array[(Seq[Message], File)] = messagesFiles.tail.map(loadMessagesFile)

				if (PlayMessagesKeys.checkDuplicateKeys.value) {
					state.log.info("Checking for duplicate keys.")
					((defaultMessages, messagesFiles.head) +: languageSpecificMessages).foreach {
						case (messages: Seq[Message], file: File) =>
							val duplicateMessageKeys = messages.groupBy(_.key).filter(_._2.length > 1).keys
							if (duplicateMessageKeys.nonEmpty) state.log.warn("Messages file contains duplicate keys: [" + file + "] [" + duplicateMessageKeys.mkString("; ") + "].")
							else state.log.debug("Messages file contains no duplicate keys: [" + file + "]")
					}
				}

				if (PlayMessagesKeys.checkKeyConsistency.value) {
					state.log.info("Checking key consistency.")
					val skipFilenames = PlayMessagesKeys.checkKeyConsistencySkipFilenames.value
					val nonexistentFilenames = skipFilenames -- messagesFiles.map(_.getName)
					if (nonexistentFilenames.nonEmpty) state.log.warn("'checkKeyConsistencySkipFilenames' contains filenames that do not exist.")
					languageSpecificMessages.filterNot(messagesAndFile => skipFilenames.contains(messagesAndFile._2.getName)).foreach {
						case (messages: Seq[Message], file: File) =>
							val currentMessageKeys = messages.map(_.key).distinct
							val missingKeys = defaultMessageKeysDistinctSorted.diff(currentMessageKeys)
							if (missingKeys.nonEmpty) {
								val msg = "Messages file is missing some keys: [" + file + "] [" + missingKeys.mkString("; ") + "]"
								if (hasDefaultMessagesFile) state.log.warn(msg)
								else {
									state.log.error(msg)
									state.status = Failed
								}
							}
							val extraKeys = currentMessageKeys.diff(defaultMessageKeysDistinctSorted)
							if (extraKeys.nonEmpty) state.log.warn("Messages file contains keys not in the default messages file: [" + file + "] [" + extraKeys.mkString("; ") + "]")
							if (missingKeys.isEmpty && extraKeys.isEmpty) state.log.debug("Messages file is ok: [" + file + "]")
					}
				}

				if (PlayMessagesKeys.checkKeysUsed.value) {
					val (_, generatedObjectName) = parseGeneratedObject(PlayMessagesKeys.generatedObject.value)

					if (!PlayMessagesKeys.generateObject.value) state.log.warn("Not checking key usage as object generation is disable.")
					else if (generatedObjectName == null) state.log.warn("Unable to check key usage due to the 'generatedObject' error above.")
					else {
						state.log.info("Checking key usage.")
						val ignoreFilenames = PlayMessagesKeys.checkKeysUsedIgnoreFilenames.value
						val ignoreKeys = PlayMessagesKeys.checkKeysUsedIgnoreKeys.value
						val messagesReferenceRegex = s"""[\\s\\.,(]$generatedObjectName\\.([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)""".r
						val sourceFiles = (Keys.unmanagedSourceDirectories in Compile).value.distinct.flatMap(listFilesRecursively).filterNot(file => ignoreFilenames.exists(file.getAbsolutePath.endsWith))
						val referencedMessageKeys = sourceFiles.flatMap { file =>
							messagesReferenceRegex.findAllMatchIn(IO.readLines(file).mkString).map(_.group(1)).toSeq
						}.distinct
						val remainingMessageKeys = defaultMessageKeysDistinctSorted.filterNot(messageKey => ignoreKeys.exists(messageKey.matches)).diff(referencedMessageKeys)
						if (remainingMessageKeys.nonEmpty) state.log.warn("Some message keys are not used: [" + remainingMessageKeys.mkString("; ") + "]")
						else state.log.debug("All messages keys that are not to be ignored are used.")
					}
				}

			}
			(state.status, defaultMessageKeysDistinctSorted)
		}
	}

	def deleteFile(file: File)(implicit state: State): Seq[File] = {
		if (file.exists) {
			file.delete
			state.log.debug("Deleted file: [" + file + "]")
		}
		Nil
	}

	def writeFile(newContent: String, file: File)(implicit state: State): Seq[File] = {
		val currentObjectContent: String = {
			if (file.exists) {
				state.log.debug("Loading existing file: [" + file + "]")
				IO.read(file)
			} else {
				state.log.debug("Creating new file: [" + file + "]")
				IO.createDirectory(file.getParentFile)
				null
			}
		}
		if (newContent != currentObjectContent) {
			IO.write(file, newContent)
			state.log.debug("Saved file: [" + file + "]")
		}

		Seq(file)
	}

	private[sbt_play_messages] val checkAndGenerateScalaTask: Def.Initialize[Task[(PlayMessagesPlugin.Status, Seq[File])]] = Def.task {
		val (status, messageKeys) = checkTask.value

		implicit val state = new State(Keys.streams.value.log)

		val (generatedObjectPackage, generatedObjectName) = parseGeneratedObject(PlayMessagesKeys.generatedObject.value)

		val generatedObjectFile = (Keys.sourceManaged in Compile).value / (PlayMessagesKeys.generatedObject.value.replace('.', '/') + ".scala")

		if (status == NoChange) {
			state.log.info("generateMessagesObject: NO CHANGE")
			(status, if (generatedObjectFile.exists) Seq(generatedObjectFile) else Nil)
		} else if (!PlayMessagesKeys.generateObject.value || messageKeys.isEmpty) {
			(status, deleteFile(generatedObjectFile))
		} else {
			state.log.info("Generating Scala object.")
			val currentObjectNesting = scala.collection.mutable.ArrayBuffer.empty[String]
			val objectContent = "" +
				// Open root object and generated classes.
				s"""package $generatedObjectPackage
				   |
				   |import com.github.evanbennett.play_messages._
				   |
				   |/** Play Message Keys Object generated by PlayMessagesPlugin. */
				   |object $generatedObjectName extends RootMessageObject {
				   |
				   |""".stripMargin +
				// Output message values.
				messageKeys.map { messageKey =>
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

			(status, writeFile(objectContent, generatedObjectFile))
		}
	}

	private[sbt_play_messages] val checkAndGenerateJavaTask: Def.Initialize[Task[(PlayMessagesPlugin.Status, Seq[File])]] = Def.task {
		implicit val state = new State(Keys.streams.value.log)

		val (status, messageKeys) = checkTask.value

		val (generatedObjectPackage, generatedObjectName) = parseGeneratedObject(PlayMessagesKeys.generatedObject.value)

		val generatedObjectFile = (Keys.sourceManaged in Compile).value / (PlayMessagesKeys.generatedObject.value.replace('.', '/') + ".java")

		if (status == NoChange) {
			state.log.info("generateMessagesObject: NO CHANGE")
			(status, if (generatedObjectFile.exists) Seq(generatedObjectFile) else Nil)
		} else if (!PlayMessagesKeys.generateObject.value || messageKeys.isEmpty) {
			(status, deleteFile(generatedObjectFile))
		} else {
			state.log.info("Generating Java object.")
			val currentObjectNesting = scala.collection.mutable.ArrayBuffer.empty[String]
			val objectContent = "" +
				// Open root object and generated classes.
				s"""package $generatedObjectPackage;
				   |
				   |import com.github.evanbennett.play_messages.Message;
				   |
				   |/*
				   | * Play Message Keys Object generated by PlayMessagesPlugin.
				   | */
				   |public final class $generatedObjectName {
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
				messageKeys.map { messageKey =>
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
								("	" * j) + s"""public static final class $requiredNesting {""" + "\n" +
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

			(status, writeFile(objectContent, generatedObjectFile))
		}
	}
}