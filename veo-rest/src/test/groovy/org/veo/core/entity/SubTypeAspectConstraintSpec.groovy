/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import javax.validation.Validation
import javax.validation.Validator

import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.SubTypeAspectData

import spock.lang.Specification
/**
 * Test {@link ProcessData} property constraints.
 */
class SubTypeAspectConstraintSpec extends Specification {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

    def "subType and status must not be null"() {
        given: "a sub type aspect without a sub type"
        def aspect = new SubTypeAspectData(Mock(Domain), Mock(Element), null, null)

        when: "it is validated"
        def errors = validator.validate(aspect)

        then: "a not-null error is present"
        errors.size() == 2
        assert errors*.propertyPath*.toString() as Set == [
            "subType",
            "status",
        ] as Set
        assert errors*.messageTemplate as Set == [
            '{javax.validation.constraints.NotNull.message}',
            '{javax.validation.constraints.NotNull.message}',
        ] as Set
    }
}
