/**
 * © 2013-4 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.play_messages

import play.api.i18n.Lang

/** Message */
class Message(val key: String) {
	if (key == null || key.isEmpty) throw new IllegalArgumentException("'key' must be provided.")

	def apply(args: String*)(implicit lang: Lang): String = play.api.i18n.Messages(key, args: _*)

	def get(args: Object*): String = play.i18n.Messages.get(key, args)

	def get(lang: Lang, args: Object*): String = play.i18n.Messages.get(lang, key, args)

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

	def apply(key: String): Message = new Message(key)

	def unapply(message: Message): Option[(String)] = {
		if (message == null) None
		else Some(message.key)
	}
}