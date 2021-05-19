/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.core

import java.time.Instant

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import org.veo.persistence.access.StoredEventRepository
import org.veo.persistence.entity.jpa.StoredEventData

@SpringBootTest(classes = StoredEventRepositorySpec.class)
@Transactional()
class StoredEventRepositorySpec extends VeoSpringSpec {

    @Autowired
    StoredEventRepository storedEventRepository

    def "finds unprocessed stored events"() {
        given:
        storedEventRepository.save(new StoredEventData().tap {
            routingKey = "a"
            processed = true
        })
        storedEventRepository.save(new StoredEventData().tap {
            routingKey = "b"
        })
        storedEventRepository.save(new StoredEventData().tap {
            routingKey = "c"
        })
        storedEventRepository.save(new StoredEventData().tap {
            routingKey = "d"
            lockTime = Instant.parse("2021-02-19T12:00:00.000Z")
        })
        storedEventRepository.save(new StoredEventData().tap {
            routingKey = "e"
            lockTime = Instant.parse("2021-02-19T14:00:00.000Z")
        })

        when:
        def pending = storedEventRepository.findPendingEvents(Instant.parse("2021-02-19T13:00:00.000Z"));

        then:
        pending.size() == 3
        pending[0].routingKey == "b"
        pending[1].routingKey == "c"
        pending[2].routingKey == "d"
    }
}
