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
package org.veo.core.entity

import org.veo.persistence.entity.jpa.ProcessData

import jakarta.validation.Validation
import jakarta.validation.Validator
import spock.lang.Specification

/**
 * Test {@link ProcessData} property constraints.
 */
class ProcessConstraintSpec extends Specification {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    def "designator, owner, and name must be specified"() {
        given: "an invalid process object"
        def id = UUID.randomUUID()
        def processData = new ProcessData()
        processData.id = id

        when: "the process is validated"
        def errors = validator.validate(processData)

        then: "not-null errors are present"
        errors.size() == 3
        assert errors*.propertyPath*.toString() as Set == [
            "designator",
            "owner",
            "name"
        ] as Set
        assert errors*.messageTemplate as Set == [
            '{jakarta.validation.constraints.NotNull.message}',
            'A name must be present.'
        ] as Set
    }
}
