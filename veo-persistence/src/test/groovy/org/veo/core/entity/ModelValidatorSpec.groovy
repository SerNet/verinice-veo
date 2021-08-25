/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import org.veo.core.entity.code.ModelValidationException
import org.veo.core.entity.code.ModelValidator
import org.veo.test.VeoSpec

// generateValidatorTest
class ModelValidatorSpec extends VeoSpec {

    def "a properly initialized Person instance passes validation"() {
        given : "a valid person"
        Unit unit = newUnit(null)
        Person person = newPerson(unit)

        when : "it is validated"
        ModelValidator.validate(person)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Asset instance passes validation"() {
        given : "a valid asset"
        Unit unit = newUnit(null)
        Asset asset = newAsset(unit)

        when : "it is validated"
        ModelValidator.validate(asset)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Process instance passes validation"() {
        given : "a valid process"
        Unit unit = newUnit(null)
        Process process = newProcess(unit)

        when : "it is validated"
        ModelValidator.validate(process)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Document instance passes validation"() {
        given : "a valid document"
        Unit unit = newUnit(null)
        Document document = newDocument(unit)

        when : "it is validated"
        ModelValidator.validate(document)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Control instance passes validation"() {
        given : "a valid control"
        Unit unit = newUnit(null)
        Control control = newControl(unit)

        when : "it is validated"
        ModelValidator.validate(control)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Domain instance passes validation"() {
        given : "a valid domain"
        Domain domain = newDomain()

        when : "it is validated"
        ModelValidator.validate(domain)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Unit instance passes validation"() {
        given : "a valid unit"
        Unit unit = newUnit(Mock(Client)) {
            parent = Mock(Unit)
        }

        when : "it is validated"
        ModelValidator.validate(unit)

        then: "the validation of is successful"
        notThrown(ModelValidationException)

    }


}
