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


import org.veo.test.VeoSpec

class DomainSpec extends VeoSpec {

    def "Create a new Domain"() {
        given: "a domain name"
        String name = 'Test domain'

        when : "Domain is created"
        Domain domain = newDomain() {
            it.name = name
        }

        and: "the domain is added to a client"
        def client = newClient() {
            domains = [domain] as Set
        }

        then: "domain is correct initatlized"
        domain.getName().equals(name)
        domain.owner == client
        client.getDomains().size() == 1
        client.getDomains().first() == domain
    }

    def "Create a new Domain linked with domain template"() {
        given: "a domain name"
        String name = 'Test domain'

        when : "Domain is created"
        Domain domain = newDomain() {
            it.name = name
        }

        and: "the domain is added to a client"
        def client = newClient() {
            domains = [domain] as Set
        }

        and: "the domain template is linked"
        def domainTemplate = newDomainTemplate() {
            it.name = 'domain template'
        }

        domain.domainTemplate = domainTemplate

        then: "domain is correct initatlized"
        domain.getName().equals(name)
        domain.owner == client
        domain.domainTemplate == domainTemplate
        client.getDomains().size() == 1
        client.getDomains().first() == domain
    }
}
