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

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.event.StoredEvent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A message about stored events.
 *
 * Receivers should bear in mind that:
 * <ul>
 * <li>messages may be send in another order than the original events occurred
 * <li>messages may be duplicated (use de-duplication in receivers)
 * </ul>
 *
 * @see StoredEvent
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter(AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventMessage {

    String routingKey;

    String content;

    @EqualsAndHashCode.Include
    Long id;

    Instant timestamp;

    public static EventMessage from(StoredEvent event) {
        return new EventMessage(event.getRoutingKey(), event.getContent(), event.getId(),
                event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
    }

    public static Set<EventMessage> messagesFrom(Collection<StoredEvent> events) {
        return events.stream()
                     .map(EventMessage::from)
                     .collect(Collectors.toSet());
    }

}
