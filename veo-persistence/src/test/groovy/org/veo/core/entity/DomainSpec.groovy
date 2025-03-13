/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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

import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.test.VeoSpec

class DomainSpec extends VeoSpec {

    def client = newClient()

    def "Create a new Domain"() {
        given: "a domain name"
        String name = 'Test domain'

        when : "Domain is created and added to the client"
        Domain domain = newDomain(client) {
            it.name = name
        }

        then: "domain is correct initatlized"
        domain.getName() == name
        domain.owner == client
        client.getDomains().size() == 1
        client.getDomains().first() == domain
    }

    def "Create a new Domain linked with domain template"() {
        given: "a domain name"
        String name = 'Test domain'

        when : "Domain is created and added to the client"
        Domain domain = newDomain(client) {
            it.name = name
            it.riskDefinitions = ["id":
                createRiskDefinition("id1")
            ] as Map
        }

        and: "the domain template is linked"
        def domainTemplate = newDomainTemplate() {
            it.name = 'domain template'
        }

        domain.domainTemplate = domainTemplate

        then: "domain is correct initatlized"
        domain.getName() == name
        domain.owner == client
        domain.domainTemplate == domainTemplate
        client.getDomains().size() == 1
        client.getDomains().first() == domain
        client.getDomains().first().riskDefinitions != null
    }

    def "ControlImplementationConfiguration with complianceControlSubType but no complianceOwnerElementTypes does not pass validation"() {
        when:
        Domain domain = newDomain(client) {
            controlImplementationConfiguration = new ControlImplementationConfiguration('FOO', null, null)
        }

        then:
        UnprocessableDataException e = thrown()
        e.message == 'complianceOwnerElementTypes must not be empty if complianceControlSubType is set.'
    }

    def "ControlImplementationConfiguration with complianceControlSubType but empty complianceOwnerElementTypes does not pass validation"() {
        when:
        Domain domain = newDomain(client) {
            controlImplementationConfiguration = new ControlImplementationConfiguration('FOO', null, [] as Set)
        }

        then:
        UnprocessableDataException e = thrown()
        e.message == 'complianceOwnerElementTypes must not be empty if complianceControlSubType is set.'
    }

    def "ControlImplementationConfiguration without complianceControlSubType but with complianceOwnerElementTypes does not pass validation"() {
        when:
        Domain domain = newDomain(client) {
            controlImplementationConfiguration = new ControlImplementationConfiguration(null, null, [ElementType.SCOPE] as Set)
        }

        then:
        UnprocessableDataException e = thrown()
        e.message == 'complianceOwnerElementTypes must be empty if complianceControlSubType is not set.'
    }

    def "ControlImplementationConfiguration with invalid complianceOwnerElementType does not pass validation"() {
        when:
        Domain domain = newDomain(client) {
            controlImplementationConfiguration = new ControlImplementationConfiguration('FOO', null, [ElementType.SCENARIO] as Set)
        }

        then:
        UnprocessableDataException e = thrown()
        e.message == 'complianceOwnerElementTypes contains invalid types.'
    }
}
