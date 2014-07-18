/**
 * © 2013-4 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.play_messages

/** RootMessageObject */
trait RootMessageObject {

	def apply(key: String, args: String*)(implicit lang: play.api.i18n.Lang): String = {
		play.api.i18n.Messages(key, args: _*)
	}
}