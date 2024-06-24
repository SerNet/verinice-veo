/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.persistence.access.jpa

import static org.veo.core.entity.event.VersioningEvent.ModificationType.PERSIST
import static org.veo.core.entity.event.VersioningEvent.ModificationType.REMOVE
import static org.veo.core.entity.event.VersioningEvent.ModificationType.UPDATE

import java.time.Instant

import org.springframework.context.ApplicationEventPublisher

import org.veo.core.entity.Asset
import org.veo.core.entity.event.ClientOwnedEntityVersioningEvent
import org.veo.core.entity.event.VersioningEvent

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class MostRecentChangeTrackerSpec extends Specification {

    @Subject
    MostRecentChangeTracker sut

    @Shared
    def uuid = UUID.randomUUID()

    def "Combine persist with update & remove events: #events into #result"() {
        setup:
        ApplicationEventPublisher publisher = Mock()
        Asset asset = Mock()
        asset.getIdAsString() >>> ids
        asset.getRequirementImplementations() >> []
        asset.getControlImplementations() >> []
        sut = new MostRecentChangeTracker(publisher)

        expect:
        def toSend = events.collect {e -> values2Event(asset, e[0], e[1]) } as List
        toSend.forEach {
            sut.put(it)
        }
        sut.consolidatedChanges.values().collectMany { list ->
            list.collect {
                event2Values(it)
            }
        } ==~ result

        where:
        /* Sadly spotless messes up the readability of these lines:
         events                                                      | result                                    | ids
         [[0, PERSIST], [1, UPDATE]]                                 | [[0, PERSIST], [1, UPDATE]]               | [null, uuid]
         [[1, UPDATE], [0, PERSIST]]                                 | [[0, PERSIST], [1, UPDATE]]               | [uuid, null]
         [[0, PERSIST], [0, PERSIST]]                                | [[0, PERSIST]]                            | [null, null]
         [[0, PERSIST], [1, UPDATE], [2, UPDATE]]                    | [[0, PERSIST], [1, UPDATE]]               | [null, uuid]
         [[0, PERSIST], [1, UPDATE], [2, UPDATE], [3, UPDATE]]       | [[0, PERSIST], [1, UPDATE]]               | [null, uuid]
         [[0, PERSIST], [1, REMOVE]]                                 | [[0, PERSIST], [1, REMOVE]]               | [null, uuid]
         [[0, PERSIST], [2, UPDATE], [3, REMOVE], [1, UPDATE]]       | [[0, PERSIST], [1, UPDATE], [2, REMOVE]]  | [null, uuid]
         */
        events                                                      | result                                    | ids
        [[0, PERSIST], [1, UPDATE]]                                 | [[0, PERSIST], [1, UPDATE]]               | [null, uuid]
        [[1, UPDATE], [0, PERSIST]]                                 | [[0, PERSIST], [1, UPDATE]]               | [uuid, null]
        [[0, PERSIST], [0, PERSIST]]                                | [[0, PERSIST], [0, PERSIST]]              | [null, null]
        [
            [0, PERSIST],
            [1, UPDATE],
            [2, UPDATE]
        ]                    | [[0, PERSIST], [1, UPDATE]]               | [null, uuid, uuid]
        [
            [0, PERSIST],
            [1, UPDATE],
            [2, UPDATE],
            [3, UPDATE]
        ]       | [[0, PERSIST], [1, UPDATE]]               | [null, uuid, uuid, uuid]
        [[0, PERSIST], [1, REMOVE]]                                 | [[0, PERSIST], [1, REMOVE]]               | [null, uuid]
        [
            [0, PERSIST],
            [2, UPDATE],
            [3, REMOVE],
            [1, UPDATE]
        ]       | [
            [0, PERSIST],
            [1, UPDATE],
            [2, REMOVE]
        ]  | [null, uuid]
    }

    def "Combine only update events: #events into #result"() {
        given:
        ApplicationEventPublisher publisher = Mock()
        Asset asset = Mock()

        asset.getIdAsString() >> uuid
        asset.getRequirementImplementations() >> []
        asset.getControlImplementations() >> []
        sut = new MostRecentChangeTracker(publisher)

        expect:
        def toSend = events.collect {e -> values2Event(asset, e[0], e[1]) } as List
        toSend.forEach {
            sut.put(it)
        }
        sut.consolidatedChanges.values().collectMany { list ->
            list.collect {
                event2Values(it)
            }
        } ==~ result

        where:
        events                                      | result
        [[1, UPDATE], [2, UPDATE]]                  | [[1, UPDATE]]
        [[1, UPDATE], [7, UPDATE]]                  | [[1, UPDATE]]
        [[3, UPDATE], [7, UPDATE]]                  | [[3, UPDATE]]
        [
            [3, UPDATE],
            [4, UPDATE],
            [7, UPDATE]
        ]                                           | [[3, UPDATE]]
        [
            [7, UPDATE],
            [4, UPDATE],
            [3, UPDATE]
        ]                                           | [[3, UPDATE]]
        [
            [4, UPDATE],
            [7, UPDATE],
            [3, UPDATE]
        ]                                           | [[3, UPDATE]]
        [
            [3, UPDATE],
            [7, UPDATE],
            [4, UPDATE]
        ]                                           | [[3, UPDATE]]
    }

    def "Combine remove events: #events into #result"() {
        given:
        ApplicationEventPublisher publisher = Mock()
        Asset asset = Mock()

        asset.getIdAsString() >> uuid
        asset.getRequirementImplementations() >> []
        asset.getControlImplementations() >> []
        sut = new MostRecentChangeTracker(publisher)

        expect:
        def toSend = events.collect {e -> values2Event(asset, e[0], e[1]) } as List
        toSend.forEach {
            sut.put(it)
        }
        sut.consolidatedChanges.values().collectMany { list ->
            list.collect {
                event2Values(it)
            }
        } ==~ result

        where:
        events                                      | result
        [[1, UPDATE], [2, REMOVE]] | [[1, UPDATE], [2, REMOVE]]
        [
            [1, UPDATE],
            [2, UPDATE],
            [3, REMOVE]
        ]                          | [[1, UPDATE], [2, REMOVE]]
        [
            [2, UPDATE],
            [1, UPDATE],
            [3, REMOVE]
        ]                          | [[1, UPDATE], [2, REMOVE]]
        [
            [7, REMOVE],
            [2, UPDATE],
            [1, UPDATE]
        ]                          | [[1, UPDATE], [2, REMOVE]]
    }

    VersioningEvent values2Event(Asset asset, int changeNo, VersioningEvent.ModificationType type) {
        return new ClientOwnedEntityVersioningEvent(asset, type, "me", Instant.now(), changeNo)
    }

    List event2Values(VersioningEvent it) {
        return [
            it.changeNumber as int,
            it.type
        ]
    }
}
