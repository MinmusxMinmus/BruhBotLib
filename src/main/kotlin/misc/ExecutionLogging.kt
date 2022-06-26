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

package rmi.misc

abstract class ExecutionEvent(val message: String)

class ExecutionError(message: String, val exception: Exception?): ExecutionEvent(message)
class ExecutionMilestone(message: String): ExecutionEvent(message)

class ExecutionLog {
    val events: MutableList<ExecutionEvent> = mutableListOf()
    val success = events.last() !is ExecutionError
    val lastError = events.last().message

    fun update(message: String) {
        events.add(ExecutionMilestone(message))
    }

    fun error(message: String, exception: Exception?) {
        events.add(ExecutionError(message, exception))
    }
}