/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ScenarioRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@SpringBootTest(classes = ScenarioRepositoryITSpec.class)
@Transactional()
class ScenarioRepositoryITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private ScenarioRepositoryImpl scenarioRepository

    private Client client
    private Unit unit

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(this.client))
    }

    def "cascading relations are validated"() {
        when:
        scenarioRepository.save(newScenario(unit) {
            customAspects = [
                newCustomAspect(null)
            ]
            links = [
                newCustomLink(null, "goodLink")
            ]
            parts = [
                newScenario(unit) {
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
