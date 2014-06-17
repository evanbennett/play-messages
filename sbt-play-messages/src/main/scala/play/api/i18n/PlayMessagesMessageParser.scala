/**
 * © 2014 Evan Bennett
 * All rights reserved.
 */

package play.api.i18n

/** Play Messages Message Parser - So that Play's Message Parser can be accessed with a java.io.File. */
object PlayMessagesMessageParser extends Messages.MessagesParser(null, null) {

	case class Message(key: String, pattern: String)

	case class MessagesAndFile(messages: Seq[Message], file: java.io.File)

	override def parse: Either[play.api.PlayException.ExceptionSource, Seq[Messages.Message]] = throw new UnsupportedOperationException

	def parse(messagesFile: java.io.File)(implicit codec: scala.io.Codec = scala.io.Codec.UTF8): Either[play.api.PlayException.ExceptionSource, Seq[Message]] = {
		val messageFileString = play.utils.PlayIO.readFileAsString(messagesFile)
		parser(new scala.util.parsing.input.CharSequenceReader(messageFileString + "\n")) match {
			case Success(messages, _) => Right(messages.map(message => Message(message.key, message.pattern)))
			case NoSuccess(message, in) => Left(
				new play.api.PlayException.ExceptionSource("Configuration error", message) {
					val line: Integer = in.pos.line
					val position: Integer = in.pos.column - 1
					val input = messageFileString
					val sourceName = messagesFile.getName
				}
			)
		}
	}
}