/*
 * Copyright © 2022 MinmusxMinmus. This file is part of "BruhBot"
 *
 * "BruhBot" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "BruhBot" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "BruhBot".  If not, see <https://www.gnu.org/licenses/>.
 */

package classicCommands

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import rmi.Logging
import rmi.logger
import java.io.Serializable

data class Parameter(
    val name: String,
    val description: String,
    val location: MessageLocation,
    val parser: ParameterParser,
): Serializable

interface ParameterResult: Serializable {
    fun error(): Boolean
    fun value(): Any
}
class NoParameter(private val parameter: Parameter): ParameterResult {
    override fun error() = true
    override fun value() = "No value for parameter '${parameter.name}'"
}
class NullParameter(private val parameter: Parameter): ParameterResult {
    override fun error() = false
    override fun value() = "Null parameter for '${parameter.name}'"
}
class StringValue(private val value: String): ParameterResult {
    override fun error() = false
    override fun value() = value
}
class BooleanValue(private val value: Boolean): ParameterResult {
    override fun error() = false
    override fun value() = value
}
class IntegerValue(private val value: Int): ParameterResult {
    override fun error() = false
    override fun value() = value
}
class DecimalValue(private val value: Double): ParameterResult {
    override fun error() = false
    override fun value() = value
}

class ParameterConfiguration(val description: String, private val separator: ArgumentSeparator, private val parameters: List<Parameter>):
    Logging, Serializable {
        companion object : Logging {
            val logger = logger()
        }
    /**
     * Parses the given message according to the [Parameter] configuration. On success, it will return a list of
     * [ParameterResult] of equal length to the configuration's parameter list. On failure, it will raise an
     * [IllegalArgumentException].
     */
    fun parse(message: Message): List<ParameterResult> {
        logger.info("Begin parsing of message")
        logger.debug("Amount of expected parameters to extract: ${parameters.size}")
        logger.debug("Sending message to separator")
        val res = mutableListOf<ParameterResult>()
        val values = separator.split(message, parameters)
        logger.debug("Separator finished splitting the message")
        logger.debug("Total amount of values obtained: ${values.size}")
        logger.info("Begin evaluating individual parameters")
        var failure = false
        for (parameter in parameters) {
            logger.debug("Evaluating parameter '${parameter.name}'")
            if (values[parameter] == null) {
                logger.error("No value found for parameter '${parameter.name}'")
                failure = true
                continue
            }
            val result = values[parameter]!!
            logger.debug("Parsing parameter '${parameter.name}'")
            logger.debug("Parser is of type ${parameter.parser::class.simpleName}")
            logger.debug("Result is of type ${result::class.simpleName}")
            val param = parameter.parser.parse(parameter, result)
            // The parameter configuration fails if any of the specified parameters is not found
            if (param is NoParameter) {
                logger.error("Parameter parsing failed")
                failure = true
                continue
            }
            res += param
        }
        if (failure) {
            logger.error("Values not found, or invalid, for certain parameters. Configuration parsing failed")
            throw IllegalArgumentException("Values not found, or invalid, for certain parameters")
        }
        logger.info("Configuration parsing successful")
        return res.toList()
    }
}

enum class MessageLocation: Serializable {
    CONTENT, ATTACHMENTS, OTHER
}

abstract class ParameterParser: Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }
    abstract fun parse(parameter: Parameter, obj: Any): ParameterResult
}
class NullParser: ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any) = NullParameter(parameter)
}
class StringParser: ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any) = StringValue(obj.toString())
}
class KeywordParser(private val keywords: Collection<String>): ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any): ParameterResult {
        with(obj as String) { for (k in keywords) if (this == k) {
            logger.debug("Keyword '$k' matched the input string")
            return StringValue(obj.toString())
        } }
        logger.warn("No keyword matched the input string $obj")
        return NoParameter(parameter)
    }
}
class BooleanParser: ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any): ParameterResult {
        // This parser only parses strict booleans from strings. Everything else is considered a failure
        return if (obj is String) {
            try {
                BooleanValue(obj.toBooleanStrict())
            } catch (e: Exception) {
                NoParameter(parameter)
            }
        } else NoParameter(parameter)
    }
}
class IntegerParser: ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any): ParameterResult {
        // This parser only parses strict integers from strings. Everything else is considered a failure
        return if (obj is String) {
            try {
                IntegerValue(obj.toInt())
            } catch (e: Exception) {
                NoParameter(parameter)
            }
        } else NoParameter(parameter)
    }
}
class DecimalParser: ParameterParser() {
    override fun parse(parameter: Parameter, obj: Any): ParameterResult {
        // This parser only parses strict doubles from strings. Everything else is considered a failure
        return if (obj is String) {
            try {
                DecimalValue(obj.toDouble())
            } catch (e: Exception) {
                NoParameter(parameter)
            }
        } else NoParameter(parameter)
    }
}

class ArgumentSeparator(
    val description: String,
    private val contentSeparator: ContentSeparator,
    private val attachmentSeparator: AttachmentSeparator,
    private val genericSeparator: GenericSeparator,
): Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }
    constructor(description: String, contentSeparator: ContentSeparator, attachmentSeparator: AttachmentSeparator) : this(description, contentSeparator, attachmentSeparator, EmptyGenericSeparator())
    constructor(description: String, contentSeparator: ContentSeparator) : this(description, contentSeparator, EmptyAttachmentSeparator(), EmptyGenericSeparator())
    constructor(description: String) : this(description, EmptyContentSeparator(), EmptyAttachmentSeparator(), EmptyGenericSeparator())

    /**
     * Separates a message into the different arguments, and summarizes it on a [Map] structure. If any parameter is not
     * found, the parsing fails and the method will return an empty map. If successful, the resulting map is guaranteed
     * to contain all parameters as keys.
     */
    fun split(message: Message, parameters: List<Parameter>): Map<Parameter, Any?> {
        logger.info("Begin argument separation")
        logger.debug("Amount of parameters to extract: ${parameters.size}")
        logger.debug("Extracting parameters from message content")
        val contentParameters = parameters.stream().filter { it.location == ParameterLocation.CONTENT }.toList()
        logger.debug("Parameters extracted from message content. Amount: ${contentParameters.size}")
        logger.debug("Extracting parameters from attachments")
        val attachmentParameters = parameters.stream().filter { it.location == ParameterLocation.ATTACHMENTS }.toList()
        logger.debug("Parameters extracted from attachments. Amount: ${attachmentParameters.size}")
        logger.debug("Extracting additional parameters")
        val otherParameters = parameters.stream().filter { it.location == ParameterLocation.OTHER }.toList()
        logger.debug("Additional parameters extracted. Amount: ${otherParameters.size}")

        val total = contentParameters.size + attachmentParameters.size + otherParameters.size

        if (total != parameters.size) {
            logger.warn("Unable to separate all parameters.")
            logger.debug("Expected ${parameters.size}, got $total")
            return mapOf()
        }
        val ret = mutableMapOf<Parameter, Any?>()

        for (entry in contentSeparator.split(message.contentRaw, contentParameters)) mergeEntry(ret, entry.key, entry.value)
        for (entry in attachmentSeparator.split(message.attachments, attachmentParameters)) mergeEntry(ret, entry.key, entry.value)
        for (entry in genericSeparator.split(message, otherParameters)) mergeEntry(ret, entry.key, entry.value)

        logger.info("Argument separation successful")
        for (e in ret) ContentSeparator.logger.debug("${e.key.name} -> '${e.value}'")
        return ret.toMap()
    }

    private fun mergeEntry(ret: MutableMap<Parameter, Any?>, key: Parameter, value: Any?) {
        // Check first for duplicates, for logging purposes
        if (ret.containsKey(key)) {
            logger.error("Conflict between arguments: a value has been replaced!")
            logger.debug("Conflicting parameter: ${key.name}")
            logger.debug("Existing value: ${ret[key]!!::class.simpleName} = '${ret[key]}'")
            logger.debug("New value: ${if (value == null) "null" else value::class.simpleName} = '${value}'")
        }
        ret[key] = value
    }
}

abstract class ContentSeparator(val description: String): Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }

    /**
     * Splits the content string into a series of parameter strings, and maps each one to its corresponding [Parameter].
     * If any parameter is not found, the method will raise an [IllegalArgumentException].
     */
    abstract fun split(content: String, parameters: List<Parameter>): Map<Parameter, String?>
}
class SpaceSeparator: ContentSeparator("Parameters are separated by spaces") {
    override fun split(content: String, parameters: List<Parameter>): Map<Parameter, String?> {
        logger.info("Begin content separation")
        logger.debug("Content separator type: ${this::class.simpleName}")
        // Every space (or group of spaces) indicates a different parameter
        val split = content.split(" ")
        logger.debug("Number of splits: ${split.size}. One should be the command name (f.e. 'b!ping')")
        val ret = mutableMapOf<Parameter, String?>()

        // Parameter count doesn't match
        if (split.size != parameters.size + 1) {
            logger.warn("The number of parameter strings found don't match the expected parameters")
            logger.debug("Expected ${parameters.size} + 1, got ${split.size}")
            throw IllegalArgumentException("Number of parameter strings found do not match the expected parameters")
        }

        // Enough parameters means they will be matched in order
        logger.debug("Matching each parameter string to the corresponding parameter")
        for (i in parameters.indices) ret[parameters[i]] = split[i + 1]
        logger.info("Content separation successful")
        return ret.toMap()
    }
}
class EmptyContentSeparator: ContentSeparator("Placeholder for when there's no parameters") {
    override fun split(content: String, parameters: List<Parameter>) = mapOf<Parameter, String>()
}

abstract class AttachmentSeparator(val description: String): Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }

    abstract fun split(attachments: List<Attachment>, parameters: List<Parameter>): Map<Parameter, Attachment?>
}
class DefaultAttachmentSeparator : AttachmentSeparator("Each attachments maps to each parameter, in order") {
    override fun split(attachments: List<Attachment>, parameters: List<Parameter>): Map<Parameter, Attachment?> {
        val ret = mutableMapOf<Parameter, Attachment?>()

        if (attachments.size != parameters.size)
            for (parameter in parameters) ret[parameter] = null
        else
            for (i in 0..parameters.size) ret[parameters[i]] = attachments[i]
        return ret.toMap()
    }
}
class EmptyAttachmentSeparator: AttachmentSeparator("Placeholder for when there's no parameters") {
    override fun split(attachments: List<Attachment>, parameters: List<Parameter>) = mapOf<Parameter, Attachment>()
}

abstract class GenericSeparator(val description: String): Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }

    abstract fun split(message: Message, parameters: List<Parameter>): Map<Parameter, Any?>
}
class EmptyGenericSeparator: GenericSeparator("Placeholder for when there's no parameters") {
    override fun split(message: Message, parameters: List<Parameter>) = mapOf<Parameter, Any>()
}