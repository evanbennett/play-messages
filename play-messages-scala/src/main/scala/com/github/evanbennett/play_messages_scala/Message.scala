/**
 * © 2013-4 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.play_messages_scala

/** Message */
class Message(val key: String) {

	def apply(args: String*)(implicit lang: play.api.i18n.Lang): String = {
		play.api.i18n.Messages(key, args: _*)
	}

	override def equals(a: Any): Boolean = {
		if (a == null) false
		else a match {
			case m: Message => key == m.key
			case _ => false
		}
	}

	override def hashCode: Int = 37 + key.hashCode

	override def toString: String = "Message [" + key + "]"
}

object Message {

	def apply(key: String): Message = {
		if (key == null || key.isEmpty) throw new IllegalArgumentException("'key' must be provided.")
		new Message(key)
	}

	def unapply(message: Message): Option[(String)] = {
		if (message == null) None
		else Some(message.key)
	}
}