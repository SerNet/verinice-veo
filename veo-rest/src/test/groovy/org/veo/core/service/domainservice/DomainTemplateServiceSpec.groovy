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
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.persistence.access.ClientRepositoryImpl

@ComponentScan("org.veo")
@WithUserDetails("user@domain.example")
class DomainTemplateServiceSpec extends VeoSpringSpec {
    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    DomainTemplateServiceImpl domainTemplateService

    def "create specific domain from template"() {
        given: "a client"

        def domainFromTemplate = null
        def client = null
        txTemplate.execute {
            client = repository.save(newClient {
                name = "Demo Client"
            })
            domainFromTemplate = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == DSGVO_TEST_DOMAIN_TEMPLATE_ID
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

    def "create a domainTemplate from domain"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            createTestDomainTemplate(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            domainFromTemplate = domainTemplateService.createDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()
        domainFromTemplate.templateVersion = "1.1.0"

        def domainTemplateFromDomain = txTemplate.execute {
            domainTemplateService.createDomainTemplateFromDomain(domainFromTemplate)
        }
        txTemplate.execute {
            domainFromTemplate = domainTemplateService.createDomain(client, domainTemplateFromDomain.idAsString)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
            // initialize lazy association
            client.domains*.domainTemplate*.name
        }
        domainFromTemplate = client.domains.find { it.domainTemplate.id == domainTemplateFromDomain.id }

        expect: 'the domain matches'
        domainFromTemplate.domainTemplate.id == domainTemplateFromDomain.id
        domainFromTemplate.templateVersion == "1.1.0"
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
            it[4].element.getSubType(domainFromTemplate).get() == "CTL_TOM"

            it[5].element.name == 'Test process-1'
        }
    }

    def "create a domainTemplate from domain twice"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            createTestDomainTemplate(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            domainFromTemplate = domainTemplateService.createDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()
        domainFromTemplate.templateVersion = "1.0"
        domainFromTemplate.revision = "2"

        txTemplate.execute {
            domainTemplateService.createDomainTemplateFromDomain(domainFromTemplate)
        }
        when:"created with same name/version/revision"
        txTemplate.execute {
            domainTemplateService.createDomainTemplateFromDomain(domainFromTemplate)
        }
        then: "an exception is thrown"
        thrown(ModelConsistencyException)
    }

    def "create a domain whose catalog contains a composite"() {
        given: "a client"
        Client client = repository.save(newClient {
            name = "Demo Client"
        })

        def domainFromTemplate = null
        txTemplate.execute {
            def testDomainVeo612TemplateId = "1624d827-299f-59d1-a1be-7064ed6f7f44"
            createTestDomainTemplate(testDomainVeo612TemplateId)
            domainFromTemplate = domainTemplateService.createDomain(client, testDomainVeo612TemplateId)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == "1624d827-299f-59d1-a1be-7064ed6f7f44"
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
            def testDomainTemplateVeo620 = "7254b28e-6804-5ea9-8905-e2a7b4883030"
            createTestDomainTemplate(testDomainTemplateVeo620)
            domainFromTemplate = domainTemplateService.createDomain(client, testDomainTemplateVeo620)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.dbId == "7254b28e-6804-5ea9-8905-e2a7b4883030"
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
