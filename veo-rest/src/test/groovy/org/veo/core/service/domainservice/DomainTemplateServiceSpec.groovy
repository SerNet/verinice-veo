/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.service.domainservice


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.service.DomainTemplateService
import org.veo.persistence.access.ClientRepositoryImpl

@ComponentScan("org.veo")
@WithUserDetails("user@domain.example")
class DomainTemplateServiceSpec extends VeoSpringSpec {
    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    DomainTemplateServiceImpl domainTemplateService

    def "create default domains from template"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainsFromTemplate = null
        txTemplate.execute {
            domainsFromTemplate = domainTemplateService.createDefaultDomains(client)
            domainsFromTemplate.forEach({client.addToDomains(it)})
            client = repository.save(client)
        }
        def domainFromTemplate = client.domains.find { it.name == "DS-GVO" }
        expect: 'the domain matches with the linked domainTemplate'
        with(domainFromTemplate) {
            domainTemplate.dbId == DomainTemplateService.DSGVO_DOMAINTEMPLATE_UUID
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            revision == domainTemplate.revision
            templateVersion == domainTemplate.templateVersion
            catalogs.size() == 1
            catalogs.first().catalogItems.size() == 9
        }
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.abbreviation }) {
            with(it[0]) {
                tailoringReferences.size()==1
                with(element) {
                    links.size()==0
                    abbreviation == 'TOM-A'
                    name == 'TOM zur Gewährleistung der Verfügbarkeit'
                    description.startsWith('Gewährleistung der Verfügbarkeit: Technische')
                }
            }
            with(it[1]) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
                element.abbreviation == 'TOM-C'
            }
            with(it[2]) {
                tailoringReferences.size() == 1
                tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
                element.abbreviation == 'TOM-E'
            }
            it[3].element.abbreviation == 'TOM-EFF'
            it[4].element.abbreviation == 'TOM-I'
            it[5].element.abbreviation == 'TOM-P'
            it[6].element.abbreviation == 'TOM-R'
            it[7].element.abbreviation == 'TOM-REC'
            it[7].tailoringReferences.size()==1
            it[7].tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL

            it[8].element.abbreviation == 'VVT'
            it[8].tailoringReferences.size() == 8
            it[8].tailoringReferences[0].referenceType == TailoringReferenceType.LINK
        }

        when: 'check the test domain'
        domainFromTemplate = client.domains.find { it.name == "test-domain" }
        then: 'the domain matches with the linked domainTemplate'
        with(domainFromTemplate) {
            domainTemplate.dbId == "2b00d864-77ee-5378-aba6-e41f618c7bad"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            revision == domainTemplate.revision
            templateVersion == domainTemplate.templateVersion
            catalogs.size() == 1
            catalogs.first().catalogItems.size() == 6
        }
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.name }) {
            with(it[0]) {
                tailoringReferences.size()==0
                with(element) {
                    links.size()==0
                    abbreviation == 'c-1'
                    name == 'Control-1'
                    description.startsWith('Lore')
                }
            }
            it[1].element.abbreviation == 'c-2'

            it[2].element.abbreviation == 'c-3'
            it[2].tailoringReferences.size()==1
            it[2].tailoringReferences[0].referenceType == TailoringReferenceType.LINK
            it[2].tailoringReferences[0].catalogItem == it[0]

            it[3].element.abbreviation == 'c-4'
            it[3].element.links.size()==0
            it[3].tailoringReferences.size()==1
            it[3].tailoringReferences[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
            it[3].tailoringReferences[0].catalogItem == it[1]

            it[4].element.abbreviation == 'cc-1'
            it[4].tailoringReferences.size()==1
            it[4].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[4].tailoringReferences.first().catalogItem == it[5]

            it[5].element.abbreviation == 'cc-2'
            it[5].tailoringReferences.size()==1
            it[5].tailoringReferences[0].referenceType == TailoringReferenceType.LINK
            it[5].tailoringReferences[0].catalogItem == it[4]
        }
    }

    def "don't create domain when client has domain"() {
        given: "a client with a domain"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        client.addToDomains(newDomain(client))


        when: 'default domains are created'
        def domainsFromTemplate = null
        txTemplate.execute {
            domainsFromTemplate = domainTemplateService.createDefaultDomains(client)
        }

        then: 'exception is thrown'
        IllegalArgumentException ex = thrown()
    }

    def "create specific domain from template"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            domainFromTemplate = domainTemplateService.createDomain(client, "00000000-0000-0000-0000-000000000001")
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == "00000000-0000-0000-0000-000000000001"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            revision == domainTemplate.revision
            templateVersion == domainTemplate.templateVersion
        }
        with (domainFromTemplate.catalogs) {
            size() == 1
            first().name == 'DSGVO-Controls'
            first().catalogItems.size() == 6
        }
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.name }) {
            it[0].element.name == 'Control-1'
            it[0].element.abbreviation == 'c-1'
            it[0].element.description.startsWith('Lore')
            it[0].tailoringReferences.size()==1
            it[0].tailoringReferences.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
            it[0].tailoringReferences.first().catalogItem == it[5]

            it[1].element.name == 'Control-2'
            it[1].element.abbreviation == 'c-2'
            it[1].tailoringReferences.size() == 0

            it[2].element.name == 'Control-3'
            it[2].element.abbreviation == 'c-3'
            it[2].tailoringReferences.size()==1
            it[2].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[2].tailoringReferences.first().catalogItem == it[0]

            it[3].element.name == 'Control-cc-1'

            it[4].element.name == 'Control-cc-2'

            it[5].element.name == 'Test process-1'
        }
    }

    def "create a domain whose catalog contains a composite"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            domainFromTemplate = domainTemplateService.createDomain(client, "ae567e90-14c4-4ceb-bd16-32eda13d1e0b")
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == "ae567e90-14c4-4ceb-bd16-32eda13d1e0b"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            revision == domainTemplate.revision
            templateVersion == domainTemplate.templateVersion
        }
        with (domainFromTemplate.catalogs) {
            size() == 1
            first().name == 'TEST-Controls'
            first().catalogItems.size() == 3
        }
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.name }) {
            it[0].element.name == 'All controls'
            it[1].element.name == 'Control-1'
            it[2].element.name == 'Control-2'
            it[0].element.parts.collect {it.name}.toSorted() == ['Control-1', 'Control-2']
        }
    }

    def "create a domain whose catalog contains a scope"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            domainFromTemplate = domainTemplateService.createDomain(client, "56596c4c-436a-40f1-9aaf-32c962f9553b")
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == "56596c4c-436a-40f1-9aaf-32c962f9553b"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            revision == domainTemplate.revision
            templateVersion == domainTemplate.templateVersion
        }
        with (domainFromTemplate.catalogs) {
            size() == 1
            first().name == 'TEST-Elements'
            first().catalogItems.size() == 3
        }
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.name }) {
            it[0].element.name == 'Asset 1'
            it[1].element.name == 'Asset 2'
            it[2].element.name == 'Scope 1'
            it[2].element.members.collect {it.name}.toSorted() == ['Asset 1', 'Asset 2']
        }
    }
}
