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

import javax.validation.ConstraintViolationException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.entity.jpa.ProcessData

/**
 * Integration test for the process repository. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = ProcessRepositoryITSpec.class)
class ProcessRepositoryITSpec extends VeoMvcSpec {

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

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(this.client))
    }

    def "try to persist an invalid process at the persistence layer"() {
        given: "an invalid process object"
        Key<UUID> id = Key.newUuid()
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
            "",
            "name"
        ] as Set
        assert cvex.constraintViolations*.messageTemplate as Set == [
            '{javax.validation.constraints.NotNull.message}',
            'Either owner or containingCatalogItem must be set',
            '{javax.validation.constraints.NotNull.message}'
        ] as Set
    }

    def "cascading relations are validated"() {
        when:
        processRepository.save(newProcess(unit) {
            customAspects = [
                newCustomAspect(null)
            ]
            links = [
                newCustomLink(null, "goodLink")
            ]
            parts = [
                newProcess(unit) {
                    designator = "super bad designator"
                }
            ]
            setSubType(null, "fun sub type")
        })
        then:
        def ex = thrown(ConstraintViolationException)
        ex.constraintViolations*.propertyPath*.toString().sort() == [
            "customAspects[].type",
            "links[].target",
            "parts[].designator",
            "subTypeAspects[].domain"
        ]
    }
}
