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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Asset
import org.veo.core.entity.Document
import org.veo.persistence.entity.jpa.AbstractJpaSpec

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException

class DesignatorSequenceRepositoryImplSpec extends AbstractJpaSpec{
    @Autowired
    EntityManager em

    DesignatorSequenceRepositoryImpl designatorSequenceRepo

    def setup() {
        designatorSequenceRepo = new DesignatorSequenceRepositoryImpl(em)
    }

    def "provides sequence values per client and object type"() {
        given:
        def clientId1 = UUID.randomUUID()
        def clientId2 = UUID.randomUUID()
        designatorSequenceRepo.createSequences(clientId1)
        designatorSequenceRepo.createSequences(clientId2)

        expect:
        designatorSequenceRepo.getNext(clientId1, Asset.TYPE_DESIGNATOR) == 1
        designatorSequenceRepo.getNext(clientId1, Asset.TYPE_DESIGNATOR) == 2
        designatorSequenceRepo.getNext(clientId2, Asset.TYPE_DESIGNATOR) == 1
        designatorSequenceRepo.getNext(clientId2, Asset.TYPE_DESIGNATOR) == 2
        designatorSequenceRepo.getNext(clientId1, Document.TYPE_DESIGNATOR) == 1
        designatorSequenceRepo.getNext(clientId1, Document.TYPE_DESIGNATOR) == 2
        designatorSequenceRepo.getNext(clientId2, Document.TYPE_DESIGNATOR) == 1
        designatorSequenceRepo.getNext(clientId2, Document.TYPE_DESIGNATOR) == 2
    }

    def "get multiple sequence values per object type"() {
        given:
        def clientId1 = UUID.randomUUID()
        designatorSequenceRepo.createSequences(clientId1)

        expect:
        designatorSequenceRepo.getNext(clientId1, Asset.TYPE_DESIGNATOR, 2) == [1, 2]
        designatorSequenceRepo.getNext(clientId1, Document.TYPE_DESIGNATOR, 4) == [1, 2, 3, 4]
        designatorSequenceRepo.getNext(clientId1, Asset.TYPE_DESIGNATOR, 3) == [3, 4, 5]
    }

    def "can't get value without creating sequence first"() {
        when:
        designatorSequenceRepo.getNext(UUID.randomUUID(), Asset.TYPE_DESIGNATOR)

        then:
        thrown(PersistenceException)
    }

    def "deletes sequence"() {
        given:
        def clientId = UUID.randomUUID()
        designatorSequenceRepo.createSequences(clientId)
        designatorSequenceRepo.deleteSequences(clientId)

        when:
        designatorSequenceRepo.getNext(clientId, Asset.TYPE_DESIGNATOR)

        then:
        thrown(PersistenceException)
    }

    def "idempotent sequence creation"() {
        given:
        def clientId = UUID.randomUUID()
        designatorSequenceRepo.createSequences(clientId)

        when:
        designatorSequenceRepo.createSequences(clientId)

        then:
        notThrown(Exception)
    }

    def "idempotent sequence deletion"() {
        given:
        def clientId = UUID.randomUUID()
        designatorSequenceRepo.createSequences(clientId)
        designatorSequenceRepo.deleteSequences(clientId)

        when:
        designatorSequenceRepo.deleteSequences(clientId)

        then:
        notThrown(Exception)
    }
}
