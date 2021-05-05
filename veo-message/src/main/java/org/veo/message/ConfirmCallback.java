/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.message;

/**
 * An interface to receive confirmations for dispatched messages. The semantics
 * are those from the AMQP publisher confirms. For the callback to occur,
 * publisher confirms have to be enabled in the application properties.
 * <p>
 * Confirmation behaviour will be as follows:
 * <ul>
 * <li>an un-routable mandatory or immediate message is confirmed right after
 * the basic.return
 * <li>a transient message is confirmed the moment it is enqueued
 * <li>a persistent message is confirmed when it is persisted to disk or when it
 * is consumed on every queue
 * </ul>
 */
@FunctionalInterface
public interface ConfirmCallback {

    /**
     * Called when a positive or negative confirmation from the broker was received.
     * Please note there are cases where this method will never be called. This may
     * happen in case of a timeout, rejection, network failure, server malfunction
     * or other events not addressed by publisher-confirms.
     *
     * @param ack
     *            {@code true} if the message was published, {@code false} otherwise
     */
    void confirm(boolean ack);

}
