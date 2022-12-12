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

import classicCommands.ClassicCommand
import classicCommands.ClassicCommandDeclaration
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject

/**
 * The module manager handles registration of RMI modules, as well as being able to remove them from the registry.
 */
object ModuleManager: Logging {
    private val logger = logger()
    private const val PORT_STUB = 0
    private const val PORT_REGISTRY = 1099

    /**
     * Registers a [Module] in the registry. In case of any errors, the method will not modify the registry, and will
     * simply log the error.
     */
    fun register(module: Module) {
        try {
            logger.info("Registering module '${module.name()}'")
            val stub = UnicastRemoteObject.exportObject(module, PORT_STUB)
            logger.debug("Stub exported to port $PORT_STUB")
            val registry = LocateRegistry.getRegistry(PORT_REGISTRY)
            logger.debug("Registry located at port $PORT_REGISTRY")
            registry.rebind(module.name(), stub)
            logger.info("Successfully registered module '${module.name()}'")
        } catch (e: Exception) {
            logger.error("Unable to export module '${module.name()}'")
            logger.error("Trace: ${e.stackTraceToString()}")
        }
    }

    /**
     * Removes a [Module] from the registry. If the module doesn't exist, or any other errors pop up, the method will
     * not modify the registry, and will simply log the error.
     */
    fun unregister(module: Module) {
        try {
            logger.info("Unregistering module '${module.name()}'")
            val registry = LocateRegistry.getRegistry(PORT_REGISTRY)
            logger.debug("Registry located at port $PORT_REGISTRY")
            registry.unbind(module.name())
            logger.info("Successfully unregistered module '${module.name()}'")
        } catch (e: Exception) {
            logger.error("Unable to unregister module '${module.name()}'")
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
interface Module: Remote {
    /**
     * Returns the module's name. This string must be a unique identifier.
     */
    @Throws(RemoteException::class)
    fun name(): String

    /**
     * Returns a collection containing all [ClassicCommandDeclaration] objects defined by the module.
     */
    @Throws(RemoteException::class)
    fun classicCommandDeclarations(): Collection<ClassicCommandDeclaration>

    /**
     * Executes the specified command, given the serialized trigger. If the command doesn't exist, does nothing. Returns
     * `true` or `false` whether the command executed anything or not.
     */
    @Throws(RemoteException::class)
    fun execute(declaration: ClassicCommandDeclaration, message: MessageOrigin): Boolean
}

/**
 * Default implementation of the [Module] interface.
 */
class DefaultModule (
    private val name: String,
    private val classicCommands: Collection<ClassicCommand>,
    intents: Collection<GatewayIntent>,
    token: String
) : Module, Logging {
    companion object : Logging {
        var jda: JDA? = null
        private val logger = logger()
    }
    init {
        jda ?: let {
            logger.warn("JDA building begin. Don't call any commands or other functions before this finishes")
            jda = JDABuilder.createDefault(token, intents).build().awaitReady()
            logger.info("JDA building finished")
        }
    }

    override fun name(): String = name

    override fun classicCommandDeclarations(): List<ClassicCommandDeclaration> =
        classicCommands.stream().map { it.declaration }.toList()

    override fun execute(declaration: ClassicCommandDeclaration, message: MessageOrigin): Boolean {
        logger.info("Attempting to execute classic command '${declaration.name}'")
        logger.debug("Evaluating all classic commands")
        for (command in classicCommands) {
            logger.debug("Evaluating command '${command.declaration.name}'")
            if (command.declaration == declaration) {
                logger.debug("Command '${command.declaration.name}' found")
                message.get(jda!!)?.queue { command.execute(it) }
                return true
            }
        }
        return false
    }
}

/**
 * Serializable class that allows passing of [Message] objects through RMI.
 * This class holds enough information to be able to retrieve a specific message using an existing JDA instance.
 */
class MessageOrigin(private val messageID: Long, private val channelID: Long, private val isPrivateChannel: Boolean): Serializable, Logging {
    companion object: Logging {
        /**
         * Transforms a [Message] into its [MessageOrigin] representation.
         */
        fun from(message: Message) = MessageOrigin(message.idLong, message.channel.idLong, message.isFromType(
            ChannelType.PRIVATE))
        private val logger = logger()
    }

    /**
     * Returns a [RestAction] containing the [Message] this object represents.
     */
    internal fun get(jda: JDA): RestAction<Message>? {
        logger.info("Begin message retrieving")
        logger.debug("Message ID: '$messageID', channel ID: '$channelID', private channel: $isPrivateChannel'")
        val channel = if (isPrivateChannel) jda.privateChannelCache.getElementById(channelID) else jda.textChannelCache.getElementById(channelID)
        if (channel == null) {
            logger.warn("Obtained channel is null, cannot retrieve the message")
            return null
        }
        logger.info("Message retrieving successful")
        return channel.retrieveMessageById(messageID)
    }
}