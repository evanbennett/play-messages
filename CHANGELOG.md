play-messages Changelog
===========================

v1.1.1-RC1 | Upcoming

 * Removed 'checkMessages' and 'generateMessagesObject' Commands.
 * Moved the Tasks so that they are available directly on the console.
 * Updated the Plugin object names for consistency.
 * Removed unnecessary 'resolvers'.

v1.1.0-RC1 | 2014-07-18

 * Detect if the message configuration and messages files have changed. If not, skip processing.
 * Added 'checkMessages' and 'generateMessagesObject' Commands.
 * Added a Status return value to the Tasks.
 * Merged the Scala and Java libraries.
 * Remove the Java specific Message class as the Scala version can be used.
 * Added a 'onNoChangeLoadDefaultMessageKeys' setting for SBT Plugins that extend this Plugin.

v1.0.0-RC2 | 2014-06-24

 * Added a new requireDefaultMessagesFile setting for projects that do not have a default messages file.
 * Implemented the checkApplicationLanguages functionality.
 * Removed the generateScala setting.

v1.0.0-RC1 | 2014-06-18

 * Initial release with support for Play framework 2.3, Scala and Java.
