/**
 * © 2013-4 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.play_messages_scala

/**
 * MessageKeyPart
 *
 * Used to reference each part of a Message key when split by '.'.
 */
abstract class MessageKeyPart(keySoFar: String) {
	if (keySoFar == null || keySoFar.isEmpty) throw new IllegalArgumentException("'keySoFar' must be provided.")
	if (keySoFar.last != '.') throw new IllegalArgumentException("'keySoFar' must end with a '.'.")

	def apply(additionalKey: String, args: String*)(implicit lang: play.api.i18n.Lang): String = {
		play.api.i18n.Messages(keySoFar + additionalKey, args: _*)
	}
}