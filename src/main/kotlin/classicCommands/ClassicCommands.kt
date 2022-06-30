/*
 * Copyright © 2021 MinmusxMinmus. This file is part of "BruhBot"
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
import rmi.Logging
import rmi.logger
import rmi.misc.ExecutionLog
import java.io.Serializable

abstract class ClassicCommand(val declaration: ClassicCommandDeclaration): Serializable, Logging {
    companion object : Logging {
        val logger = logger()
    }
    val status = ExecutionLog()
    var parameters = ClassicCommandParameters(listOf(), -1)

    fun execute(message: Message) {
        // Permission check
        update("Begin command permission check")
        // This is terrible, but fuck you
        if (!declaration.requirements.check(message)) {
            error("Permission check failed", null)
            execWhenBadPerms(message)
            return
        }

        update("Finished command permission check")

        // Argument check 1:
        update("Begin command argument check")
        parameters = declaration.signature.parse(message)
        if (parameters.option == -1) {
            error("Argument check failed. Command cannot execute", null)
            execWhenBadArgs(message)
            return
        }
        update("Finished command argument check")

        // Actual command
        try {
            update("Begin command execution")
            exec(message)
            update("Command executed successfully")
        } catch (e: Exception) {
            error("Exception '${e.javaClass}' caused command termination.", e)
        }
    }

    protected fun debug(message: String) {
        logger.debug(message)
        status.update(message)
    }

    protected fun update(message: String) {
        logger.info(message)
        status.update(message)
    }

    protected fun error(message: String, exception: Exception?) {
        logger.error(message)
        status.error(message, exception)
    }

    abstract fun execWhenBadArgs(message: Message)
    abstract fun execWhenBadPerms(message: Message)
    abstract fun exec(message: Message)
}

data class ClassicCommandDeclaration(val name: String,
                                     val description: String,
                                     val requirements: MessageRequirement,
                                     val signature: ClassicCommandSignature
): Serializable

class ClassicCommandParameters(val list: List<ParameterResult>, val option: Int)

class ClassicCommandSignature(private val alternatives: List<ParameterConfiguration>): Logging, Serializable {
    companion object : Logging {
        val logger = logger()
    }

    /**
     * Attempts to extract [ClassicCommandParameters] from a [Message], given a series of alternatives. Iterates through
     * the [ParameterConfiguration] objects until a valid configuration is found. If no valid configuration is found,
     * it instead returns a [ClassicCommandParameters] object with `option == -1`.
     */
    fun parse(message: Message): ClassicCommandParameters {
        logger.info("Begin message parsing")
        logger.debug("Number of configurations available: ${alternatives.size}")
        for (i in alternatives.indices) {
            logger.debug("Attempting message parsing using alternative $i")
            try {
                val res = alternatives[i].parse(message)
                logger.info("Message parsing successful using alternative $i")
                return ClassicCommandParameters(res, i)
            } catch (e: IllegalArgumentException) {
                logger.warn("Message parsing unsuccessful using alternative $i, attempting another alternative")
            }
         }
        // No alternative was valid
        logger.error("Message parsing unsuccessful after trying all alternatives")
        return ClassicCommandParameters(listOf(), -1)
    }
}