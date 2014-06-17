/**
 * © 2013-4 Evan Bennett
 * All rights reserved.
 */

package com.github.evanbennett.play_messages_java;

/** Message */
public class Message extends com.github.evanbennett.play_messages_scala.Message {

	private final String key;

	public Message(String key) {
		super(key);
		if (key == null || key.length() < 1) throw new IllegalArgumentException("'key' must be provided.");
		this.key = key;
	}

	public String get(Object... args) {
		return play.i18n.Messages.get(key, args);
	}

	public String get(play.api.i18n.Lang lang, Object... args) {
		return play.i18n.Messages.get(lang, key, args);
	}
}
