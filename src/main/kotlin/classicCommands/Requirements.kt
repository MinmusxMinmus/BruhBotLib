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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import java.io.Serializable

/**
 * A `MessageRequirement` represents a condition that a [Message] must fulfill in order for it to be treated by any sort
 * of command.
 */
interface MessageRequirement: Serializable {
    fun check(message: Message): Boolean
    infix fun and(r2: MessageRequirement): MessageRequirement = AllRequirements(listOf(this, r2))
    infix fun or(r2: MessageRequirement): MessageRequirement = AnyRequirements(listOf(this, r2))
}

/**
 * A condition that only fulfills when all conditions passed as arguments are fulfilled as well.
 */
class AllRequirements(private val requirements: Collection<MessageRequirement>) : MessageRequirement {
    override fun check(message: Message): Boolean {
        return requirements.all { it.check(message) }
    }
}

/**
 * A condition that only fulfills when any of the conditions passed as arguments is fulfilled.
 */
class AnyRequirements(private val requirements: Collection<MessageRequirement>) : MessageRequirement {
    override fun check(message: Message): Boolean {
        return requirements.any { it.check(message) }
    }
}

/**
 * A condition that is always fulfilled.
 */
class NoRequirements : MessageRequirement {
    override fun check(message: Message) = true
}

/**
 * A condition that is fulfilled when the message's author's user ID corresponds to the supplemented [userid].
 */
class RequireUser(private val userid: Long) : MessageRequirement {
    override fun check(message: Message) = message.author.idLong == userid
}

/**
 * A condition that is fulfilled when the channel where the message was sent corresponds to the supplemented
 * [channelId]
 */
class RequireChannel(private val channelId: Long) : MessageRequirement {
    override fun check(message: Message) = message.channel.idLong == channelId
}

/**
 * A condition that is fulfilled when the [Guild] where the message was sent corresponds to the supplemented [guildId]
 */
class RequireGuild(private val guildId: Long) : MessageRequirement {
    override fun check(message: Message) = message.guild.idLong == guildId
}

/**
 * A condition that is fulfilled whenever the message's author has administrator privileges in the corresponding guild.
 */
class RequireAdmin : MessageRequirement {
    override fun check(message: Message) = message.guild.getMemberById(message.author.id)?.roles?.any { it.permissions.contains(Permission.ADMINISTRATOR) } ?: false
}

/**
 * A condition that is fulfilled whenever the message's author has a role that corresponds to the supplemented [roleId].
 */
class RequireRole(private val roleId: Long) : MessageRequirement {
    override fun check(message: Message) = message.guild.getMemberById(message.author.id)?.roles?.any { it.idLong == roleId } ?: false
}

/**
 * A condition that is fulfilled when the guild the message was sent in has the supplemented [user] as a member.
 */
class RequireMember(private val user: User) : MessageRequirement {
    override fun check(message: Message) = message.guild.isMember(user)
}

/**
 * A condition that is fulfilled whenever the message was sent in a channel of the same [ChannelType] as the
 * supplemented [channelType]
 */
class RequireChannelType(private val channelType: ChannelType) : MessageRequirement {
    override fun check(message: Message) = message.channel.type == channelType
}