/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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

import org.veo.core.entity.code.ModelValidationException
import org.veo.core.entity.code.ModelValidator
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

// generateValidatorTest
class ModelValidatorSpec extends VeoSpec {
    private EntityFactory entityFactory = new EntityDataFactory()

    def "a properly initialized Person instance passes validation"() {
        given : "a valid modelobject"
        Unit unit = newUnit(null)
        Person person = newPerson(unit)

        when : "model and validator is created"
        ModelValidator.validate(person)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Asset instance passes validation"() {
        given : "a valid modelobject"
        Unit unit = newUnit(null)
        Asset asset = newAsset(unit)

        when : "model and validator is created"
        ModelValidator.validate(asset)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Process instance passes validation"() {
        given : "a valid modelobject"
        Unit unit = newUnit(null)
        Process process = newProcess(unit)

        when : "model and validator is created"

        ModelValidator.validate(process)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Document instance passes validation"() {
        given : "a valid modelobject"

        Unit unit = newUnit(null)
        Document document = newDocument(unit)

        when : "model and validator is created"
        ModelValidator.validate(document)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Control instance passes validation"() {
        given : "a valid modelobject"
        Unit unit = newUnit(null)
        Control control = newControl(unit)

        when : "model and validator is created"
        ModelValidator.validate(control)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Domain instance passes validation"() {
        given : "a valid modelobject"
        Domain domain = newDomain()

        when : "model and validator is created"
        ModelValidator.validate(domain)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Unit instance passes validation"() {
        given : "a valid modelobject"
        Unit unit = newUnit(Mock(Client)) {
            parent = Mock(Unit)
        }

        when : "model and validator is created"
        ModelValidator.validate(unit)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }


}
