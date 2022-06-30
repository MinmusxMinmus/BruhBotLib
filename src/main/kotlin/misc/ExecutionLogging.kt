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

import classicCommands.ClassicCommand
import java.time.Instant

/**
 * An `ExecutionEvent` is the base class representing any notable event that has occurred during execution. Each event
 * carries an [Instant] timestamp, as well as a description.
 */
abstract class ExecutionEvent(val message: String, val timestamp: Instant = Instant.now())

/**
 * An `ExecutionError` event indicates that there has been an error. It additionally includes space for an [Exception].
 */
class ExecutionError(message: String, val exception: Exception?): ExecutionEvent(message)

/**
 * An `ExecutionMilestone` indicates a normal event.
 */
class ExecutionMilestone(message: String): ExecutionEvent(message)

/**
 * An `ExecutionLog` instance holds a list of [ExecutionEvent] objects, each related to a specific process like a
 * [ClassicCommand]. Interaction with the event list is done through the [update], [error] and [events] functions.
 */
class ExecutionLog {
    private val events: MutableList<ExecutionEvent> = mutableListOf()
    val success = try {
        events.last() !is ExecutionError
    } catch (e: NoSuchElementException) {
        false
    }
    val lastError = try {
        events.last().message
    } catch (e: NoSuchElementException) {
        ""
    }

    /**
     * Adds a new milestone entry to the event log.
     */
    fun update(message: String) {
        events.add(ExecutionMilestone(message))
    }

    /**
     * Adds a new error entry to the event log.
     */
    fun error(message: String, exception: Exception?) {
        events.add(ExecutionError(message, exception))
    }

    /**
     * Returns an unmodifiable list containing all recorded events.
     */
    fun events(): List<ExecutionEvent> = events.toList()
}