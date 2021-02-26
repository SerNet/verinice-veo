/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity

import javax.validation.Validation
import javax.validation.Validator

import org.veo.persistence.entity.jpa.ProcessData

import spock.lang.Specification
/**
 * Test {@link ProcessData} property constraints.
 */
class ProcessConstraintSpec extends Specification {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    def "owner and name must be specified"() {
        given: "an invalid process object"
        Key<UUID> id = Key.newUuid()
        def processData = new ProcessData()
        processData.id = id
        processData.version("john.doe", null)

        when: "the process is saved using the repository"
        def errors = validator.validate(processData)

        then: "the process is not saved"
        errors.size() == 2

        and: "the reason is given"
        errors.size() == 2
        assert errors*.propertyPath*.toString() as Set == ["owner", "name"] as Set
        assert errors*.messageTemplate.every() {it == "{javax.validation.constraints.NotNull.message}" }
    }
}
