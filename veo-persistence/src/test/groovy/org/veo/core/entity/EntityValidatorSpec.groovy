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

import org.veo.core.entity.code.EntityValidationException
import org.veo.core.entity.specification.EntityValidator
import org.veo.test.VeoSpec

class EntityValidatorSpec extends VeoSpec {
    private def client = newClient()
    private def domain = newDomain(client)
    private def otherClient = newClient()
    private def accountProvider = Mock(AccountProvider) {
        currentUserAccount >> Mock(Account) {
            it.client >> client
        }
    }
    private def validator = new EntityValidator(accountProvider)
    private def unit = newUnit(client)
    private def otherUnit = newUnit(otherClient)

    def "a properly initialized Person instance passes validation"() {
        given : "a valid person"
        Person person = newPerson(unit) {
            associateWithDomain(domain, "NormalPerson", "NEW")
        }

        when : "it is validated"
        validator.validate(person)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Person with the wrong client does not pass validation"() {
        given : "a person for another client"
        Person person = newPerson(otherUnit)

        when : "it is validated"
        validator.validate(person)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Asset instance passes validation"() {
        given : "a valid asset"
        Asset asset = newAsset(unit) {
            associateWithDomain(domain, "NormalAsset", "NEW")
        }

        when : "it is validated"
        validator.validate(asset)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "an Asset with the wrong client does not pass validation"() {
        given : "an asset for another client"
        Asset person = newAsset(otherUnit)

        when : "it is validated"
        validator.validate(person)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Process instance passes validation"() {
        given : "a valid process"
        Process process = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }

        when : "it is validated"
        validator.validate(process)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Process with the wrong client does not pass validation"() {
        given : "a process for another client"
        Process process = newProcess(otherUnit)

        when : "it is validated"
        validator.validate(process)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Document instance passes validation"() {
        given : "a valid document"
        Document document = newDocument(unit) {
            associateWithDomain(domain, "NormalDocument", "NEW")
        }

        when : "it is validated"
        validator.validate(document)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Document with the wrong client does not pass validation"() {
        given : "a person for another client"
        Document document = newDocument(otherUnit)

        when : "it is validated"
        validator.validate(document)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Control instance passes validation"() {
        given : "a valid control"
        Control control = newControl(unit) {
            associateWithDomain(domain, "NormalControl", "NEW")
        }

        when : "it is validated"
        validator.validate(control)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Control with the wrong client does not pass validation"() {
        given : "a control for another client"
        Control control = newControl(otherUnit)

        when : "it is validated"
        validator.validate(control)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Domain instance passes validation"() {
        given : "a valid domain"
        Domain domain = newDomain(client)

        when : "it is validated"
        validator.validate(domain)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Domain with the wrong client does not pass validation"() {
        given : "a domain for another client"
        Domain domain = newDomain(otherClient)

        when : "it is validated"
        validator.validate(domain)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a properly initialized Unit instance passes validation"() {
        given : "a valid unit"
        Unit unit = newUnit(client) {
            parent = unit
        }

        when : "it is validated"
        validator.validate(unit)

        then: "the validation of is successful"
        notThrown(EntityValidationException)
    }

    def "a Unit with the wrong client does not pass validation"() {
        given : "a unit for another client"
        Unit unit = newUnit(otherClient)

        when : "it is validated"
        validator.validate(unit)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "A custom aspect with a wrong domain does not pass validation"() {
        given : "an entity with a custom aspect in the wrong domain"
        Domain otherDomain = newDomain(client)
        Process process = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }
        // bypass apply method to sneak in invalid CA
        process.customAspects = [
            newCustomAspect("a", otherDomain)
        ]

        when : "it is validated"
        validator.validate(process)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "A custom link with a wrong domain does not pass validation"() {
        given : "an entity with a custom link in the wrong domain"
        Domain otherDomain = newDomain(client)
        Process process = newProcess(unit) {
            associateWithDomain(domain, "NormalProcess", "NEW")
        }
        Control control = newControl(unit) {
            associateWithDomain(domain, "NormalControl", "NEW")
        }
        process.applyLink( newCustomLink(control, "a", otherDomain))

        when : "it is validated"
        validator.validate(process)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a Person with a domain in the wrong client does not pass validation"() {
        given : "a person for another client"
        Domain otherDomain = newDomain(otherClient)
        Person person = newPerson(unit) {
            associateWithDomain(otherDomain, "NormalPerson", "NEW")
        }

        when : "it is validated"
        validator.validate(person)

        then: "the validation is unsuccessful"
        thrown(EntityValidationException)
    }

    def "a person associated with a domain that's not in the unit does not pass validation"() {
        given: "a person associated with a new domain"
        Person person = newPerson(unit) {
            associateWithDomain(newDomain(client), "NormalPerson", "NEW")
        }

        when:
        validator.validate(person)

        then:
        thrown(EntityValidationException)
    }

    def "a person associated with an inactive domain does not pass validation"() {
        given:
        domain.active = false
        Person person = newPerson(unit) {
            associateWithDomain(domain, "NormalPerson", "NEW")
        }

        when:
        validator.validate(person)

        then:
        thrown(EntityValidationException)
    }

    def "a person referencing a catalog item from an unassociated domain does not pass validation"() {
        given:
        Person person = newPerson(unit) {
            associateWithDomain(domain, "NormalPerson", "NEW")
            appliedCatalogItems = [
                newCatalogItem(newDomain(client))
            ]
        }

        when:
        validator.validate(person)

        then:
        thrown(EntityValidationException)
    }

    def "a person referencing multiple catalog items from the same domain does not pass validation"() {
        given:
        Person person = newPerson(unit) {
            associateWithDomain(domain, "NormalPerson", "NEW")
            appliedCatalogItems = [
                newCatalogItem(domain),
                newCatalogItem(domain),
            ]
        }

        when:
        validator.validate(person)

        then:
        thrown(EntityValidationException)
    }

    def "a unit using another client's domain does not pass validation"() {
        given: "a unit using another client's domain"
        def unit = newUnit(client) {
            domains = [newDomain(newClient())]
        }

        when:
        validator.validate(unit)

        then:
        thrown(EntityValidationException)
    }

    def "element with part in other unit does not pass validation"() {
        given:
        def composite = newDocument(unit) {
            parts = [newDocument(newUnit(client))]
        }

        when:
        validator.validate(composite)

        then:
        thrown(EntityValidationException)
    }
}
