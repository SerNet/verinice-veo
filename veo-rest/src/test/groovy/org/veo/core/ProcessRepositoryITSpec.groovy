/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.entity.jpa.ProcessData

import jakarta.validation.ConstraintViolationException

/**
 * Integration test for the process repository. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
class ProcessRepositoryITSpec extends VeoSpringSpec {

    @Autowired
    private ProcessRepositoryImpl processRepository
    @Autowired
    ProcessDataRepository processDataRepository
    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    TransactionTemplate txTemplate

    private Client client
    private Unit unit
    private Domain domain

    def setup() {
        client = clientRepository.save(newClient {
            domain = newDomain(it)
        })
        unit = unitRepository.save(newUnit(this.client))
    }

    def "try to persist an invalid process at the persistence layer"() {
        given: "an invalid process object"
        def processData = new ProcessData()

        when: "the process is saved using the repository"
        processDataRepository.save(processData)

        then: "the process is not saved"
        TransactionSystemException ex = thrown()

        and: "the reason is given"
        ConstraintViolationException cvex = ex.mostSpecificCause
        cvex.constraintViolations.size() == 3
        assert cvex.constraintViolations*.propertyPath*.toString() as Set == [
            "designator",
            "owner",
            "name"
        ] as Set
        assert cvex.constraintViolations*.messageTemplate as Set == [
            '{jakarta.validation.constraints.NotNull.message}',
            'A name must be present.'
        ] as Set
    }

    def "cascading relations are validated"() {
        when:
        processRepository.save(newProcess(unit) {
            associateWithDomain(newDomain(client), null, null)
            // bypass apply method to sneak in invalid custom aspect
            customAspects = [newCustomAspect(null, domain)]
            applyLink(newCustomLink(null, "goodLink", domain))
            parts = [
                newProcess(unit) {
                    designator = "super bad designator"
                }
            ]
        })

        then:
        def ex = thrown(ConstraintViolationException)
        ex.constraintViolations*.propertyPath*.toString().sort() == [
            "customAspects[].type",
            "links[].target",
            "parts[].designator",
            "subTypeAspects[].status",
            "subTypeAspects[].subType",
        ]
    }
}
