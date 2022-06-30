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
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.io.Serializable

interface MessageRequirement: Serializable {
    fun check(message: Message): Boolean
    infix fun and(r2: MessageRequirement): MessageRequirement = AllRequirements(listOf(this, r2))
    infix fun or(r2: MessageRequirement): MessageRequirement = AnyRequirements(listOf(this, r2))
}

class AllRequirements(private val requirements: Collection<MessageRequirement>) : MessageRequirement {
    override fun check(message: Message): Boolean {
        return requirements.all { it.check(message) }
    }
}
class AnyRequirements(private val requirements: Collection<MessageRequirement>) : MessageRequirement {
    override fun check(message: Message): Boolean {
        return requirements.any { it.check(message) }
    }
}
class NoRequirements : MessageRequirement {
    override fun check(message: Message) = true
}

class RequireUser(private val userid: Long) : MessageRequirement {
    override fun check(message: Message) = message.author.idLong == userid
}
class RequireChannel(private val channelId: Long) : MessageRequirement {
    override fun check(message: Message) = message.channel.idLong == channelId
}
class RequireGuild(private val guildId: Long) : MessageRequirement {
    override fun check(message: Message) = message.guild.idLong == guildId
}

class RequireAdmin : MessageRequirement {
    override fun check(message: Message) = message.guild.getMemberById(message.author.id)?.roles?.any { it.permissions.contains(Permission.ADMINISTRATOR) } ?: false
}
class RequireRole(private val roleId: Long) : MessageRequirement {
    override fun check(message: Message) = message.guild.getMemberById(message.author.id)?.roles?.any { it.idLong == roleId } ?: false
}
class RequireMember(private val user: User) : MessageRequirement {
    override fun check(message: Message) = message.guild.isMember(user)
}
class RequireChannelType(private val channelType: ChannelType) : MessageRequirement {
    override fun check(message: Message) = message.channel.type == channelType
}