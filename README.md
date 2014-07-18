sbt-play-messages
=================

An SBT plugin to check Play messages files for potential problems and generate a references object.

This plugin can:
 * Check for duplicate message keys in the messages files
 * Check for missing keys in the language specific messages files
 * Check for message keys not in the default messages file
 * Check for unused message keys
 * Generate a references object, which can:
    * Provide simpler more accurate access to message keys
    * Convert message key typing mistakes into a compilation error
    * Convert removed message usage into a compilation error
    * Convert changed message keys not updated into a compilation error

Compatibility
-------------

Can be used with the [Play Framework][play] 2.3.x.

Requires [SBT][sbt] 0.13.5.

Generates code in Scala or Java.

Adding the plugin
-----------------

Add the plugin to `project/plugins.sbt`. For example:

```scala
addSbtPlugin("com.github.evanbennett" % "sbt-play-messages" % "1.1.0-RC1")
```

Your project's build needs to enable a Play SBT plugin. For example in your build.sbt:

for Scala:

```scala
lazy val root = (project in file(".")).enablePlugins(PlayScala)
```

or for Java:

```scala
lazy val root = (project in file(".")).enablePlugins(PlayJava)
```

To enable seemless usage in your Twirl templates, you could add the following line into your build.sbt:

```scala
TwirlKeys.templateImports := (TwirlKeys.templateImports.value.diff(Seq("play.api.i18n._")) ++ Seq("conf.Messages", "play.api.i18n.Lang"))
```

SBT usage
---------

The checking and generation functionality is automatically run whenever your project is compiled. You can also manually run the functionality using the following:

| Command                | Description                                               |
| ---------------------- | --------------------------------------------------------- |
| checkMessages          | Check the project's message setting and messages files.   |
| generateMessagesObject | Check the messages and then generate the messages object. |

Recommended usage of the generated object
-----------------------------------------

Import the generated object, instead of the Play 'Messages' object, and then usage is very similar to usage without this plugin.

Note: Methods are provided on the generated object, which allow existing code using the Play Messages object to compile.

**Importing the generated object:**

In a Scala file you would probably:

```scala
import play.api.i18n.Messages // Or with the wildcard '_'
```

and in a Java file you would probably:

```java
import play.i18n.Messages; // Or with the wildcard '*'
```

To use the generated object, replace this with:

```scala
import conf.Messages
```

**With a 'messages' file containing:**

```
WELCOME=Welcome to our system.
ERRORS.INCORRECT_LENGTH={0} is the incorrect length.
```

**Scala recommended usage:**

Standard Play 'Messages' usage (which can still be used):

```scala
Messages("WELCOME")
Messages("ERRORS.INCORRECT_LENGTH", 2)
```

Equivalent usage with the plugin:

```scala
Messages.WELCOME()
Messages.ERRORS.INCORRECT_LENGTH(2)
```

You also have the option of using part of the key literally and part variably (obviously, the variable part could not exist):

```scala
val error = "INCORRECT_LENGTH" // Generated dynamically from some code

Messages.ERRORS(error, 2)
```

**Java recommended usage:**

Standard Play 'Messages' usage (which can still be used):

```java
Messages.get("WELCOME");
Messages.get("ERRORS.INCORRECT_LENGTH", 2);
```

Equivalent usage with the plugin:

```java
Messages.WELCOME.get();
Messages.ERRORS.INCORRECT_LENGTH.get(2);
```

You also have the option of using part of the key literally and part variably (obviously, the variable part could not exist):

```java
String error = "INCORRECT_LENGTH"; // Generated dynamically from some code

Messages.ERRORS.get(error, 2);
```

Configuration
-------------

The following settings can be modified by adding the provided line to your build.sbt file.

### Require Default Message File

The requiring of a default messages file can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.requireDefaultMessagesFile := false
```

### Application Languages Checking

Checking that the 'application.langs' configuration matches the messages files can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.checkApplicationLanguages := false
```

### Duplicate Key Checking

Duplicate key checking can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.checkDuplicateKeys := false
```

### Key Consistency Checking

Key consistency checking across multiple messages files can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.checkKeyConsistency := false
```

You can configure some messages files to be skipped by adding:

```scala
PlayMessagesKeys.checkKeyConsistencySkipFilenames := Set("messages.en-AU")
```

### Key Usage Checking

Key usage checking throughout your application code can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.checkKeysUsed := false
```

You can configure some keys to be ignored by adding:

```scala
PlayMessagesKeys.checkKeysUsedIgnoreKeys := Set("")
```

You can use regular expressions to configure keys to be ignored. For example, to ignore all keys starting with "PERMISSIONS", you could add:

```scala
PlayMessagesKeys.checkKeysUsedIgnoreFilenames := Set("PERMISSIONS.+")
```

### Generate Key References Object

Key references object generation can be disabled by adding the following to your build configuration:

```scala
PlayMessagesKeys.generateObject := false
```

You can configure the package and name of the object that will contain the key references by adding:

```scala
PlayMessagesKeys.generatedObject := "base.Messages"
```

License
-------

This code is licensed under the [BSD 3-Clause License][bsd3clause].

[play]: http://www.playframework.com/
[sbt]: http://www.scala-sbt.org/
[bsd3clause]: http://opensource.org/licenses/BSD-3-Clause