/*
 * Copyright © 2021 MinmusxMinmus. This file is part of "BruhBot"
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
package rmi

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import rmi.simpleCommands.ClassicCommand
import rmi.simpleCommands.ClassicCommandDeclaration
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject

object ModuleManager: Logging {
    private val logger = logger()
    private const val PORT_STUB = 0
    private const val PORT_REGISTRY = 1099

    fun register(module: Module) {
        try {
            logger.info("Registering module '${module.name}'")
            val stub = UnicastRemoteObject.exportObject(module, PORT_STUB) as Module
            logger.debug("Stub exported to port $PORT_STUB")
            val registry = LocateRegistry.getRegistry(PORT_REGISTRY)
            logger.debug("Registry located at port $PORT_REGISTRY")
            registry.rebind(module.name, stub)
            logger.info("Successfully registered module '${module.name}'")
        } catch (e: Exception) {
            logger.error("Unable to export module '${module.name}'")
            logger.error("Trace: ${e.stackTraceToString()}")
        }
    }

    fun unregister(module: Module) {
        try {
            logger.info("Unregistering module '${module.name}'")
            val registry = LocateRegistry.getRegistry(PORT_REGISTRY)
            logger.debug("Registry located at port $PORT_REGISTRY")
            registry.unbind(module.name)
            logger.info("Successfully unregistered module '${module.name}'")
        } catch (e: Exception) {
            logger.error("Unable to unregister module '${module.name}'")
            logger.error("Trace: ${e.stackTraceToString()}")
        }
    }
}

/**
 * This interface serves to define all possible bot modules.
 *
 * A module is defined as an independent executable that can be remotely invoked by the bot, offering additional
 * functionality that is not necessary for basic execution. An example would be a module that only
 * offers additional commands to the bot.
 *
 * [name] needs to be a unique identifier for the module.
 */
abstract class Module(
    val name: String,

    // Addon functionalities
    private val classicCommands: Collection<ClassicCommand>,

    // JDA initializaton
    intents: Collection<GatewayIntent>,
    token: String) : Remote, Logging {
    companion object : Logging {
        var jda: JDA? = null
        private val logger = logger()
    }

    // Visible command details
    val classicCommandDeclarations = classicCommands.stream().map { it.declaration }.toList()

    init {
        jda ?: let {
            logger.warn("JDA building begin. Don't call any commands or other functions before this finishes")
            jda = JDABuilder.createDefault(token, intents).build().awaitReady()
            logger.warn("JDA building finished")
        }
    }

    /**
     * Executes the specified command, given the serialized trigger. If the command doesn't exist, does nothing.
     */
    @Throws(RemoteException::class)
    fun execute(declaration: ClassicCommandDeclaration, message: MessageOrigin): Boolean {
        for (command in classicCommands) {
            if (command.declaration == declaration) {
                message.get(jda!!)?.queue { command.execute(it) }
                return true
            }
        }
        return false
    }
}

class MessageOrigin(private val messageID: Long, private val channelID: Long, private val isPrivateChannel: Boolean): Serializable {
    companion object {
        fun from(message: Message) = MessageOrigin(message.idLong, message.channel.idLong, message.isFromType(
            ChannelType.PRIVATE))
    }

    internal fun get(jda: JDA): RestAction<Message>? {
        val channel = if (isPrivateChannel) jda.privateChannelCache.getElementById(channelID) else jda.textChannelCache.getElementById(channelID)
        return channel?.retrieveMessageById(messageID)
    }
}