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
import spock.lang.Specification

// generateValidatorTest
class ModelValidatorSpec extends Specification {
    private EntityFactory entityFactory = new EntityDataFactory()

    def "a properly initialized Person instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Person"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)


        when : "model and validator is created"

        Person person = entityFactory.createPerson(null, name, null)


        person.setOwner(unit)

        ModelValidator.validate(person)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Asset instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Asset"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)

        when : "model and validator is created"

        Asset asset = entityFactory.createAsset(Key.newUuid(), name, null)

        asset.setOwner(unit)

        ModelValidator.validate(asset)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Process instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Process"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)

        when : "model and validator is created"

        Process process = entityFactory.createProcess(Key.newUuid(), name, null)

        process.setOwner(unit)

        ModelValidator.validate(process)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Document instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Document"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)

        when : "model and validator is created"

        Document document = entityFactory.createDocument(Key.newUuid(), name, null)

        document.setOwner(unit)

        ModelValidator.validate(document)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Control instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Control"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)

        when : "model and validator is created"

        Control control = entityFactory.createControl(Key.newUuid(), name, null)

        control.setOwner(unit)

        ModelValidator.validate(control)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Domain instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Domain"



        when : "model and validator is created"

        Domain domain = entityFactory.createDomain(Key.newUuid(), name)



        ModelValidator.validate(domain)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }
    def "a properly initialized Unit instance passes validation"() {
        given : "a valid modelobject"

        String name = "model-name:Unit"



        when : "model and validator is created"

        Unit unit = entityFactory.createUnit(Key.newUuid(), name, null)

        unit.setParent(Mock(Unit))
        unit.setClient(Mock(Client))


        ModelValidator.validate(unit)

        then: "the validation of is succesful"
        notThrown(ModelValidationException)

    }


}
