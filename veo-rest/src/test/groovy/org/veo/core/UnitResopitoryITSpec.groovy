/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import org.veo.core.repository.ClientRepository
import org.veo.core.repository.UnitRepository
import org.veo.persistence.entity.jpa.ClientData

@SpringBootTest(classes = UnitResopitoryITSpec.class)
class UnitResopitoryITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepository clientRepository
    @Autowired
    private UnitRepository unitRepository
    private ClientData client

    def setup() {
        client = clientRepository.save(newClient())
    }

    def "sub units are validated"() {
        when:
        unitRepository.save(newUnit(client) {
            units = [
                newUnit(null)
            ]
        })
        then:
        def ex = thrown(ConstraintViolationException)
        with(ex.constraintViolations) {
            size() == 1
            first().propertyPath.toString() == "units[].client"
        }
    }
}
