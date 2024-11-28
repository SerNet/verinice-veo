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
import org.veo.core.entity.Domain
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.exception.ModelConsistencyException
import org.veo.core.usecase.service.DomainStateMapper
import org.veo.persistence.access.ClientRepositoryImpl

@ComponentScan("org.veo")
@WithUserDetails("user@domain.example")
class DomainTemplateServiceSpec extends VeoSpringSpec {
    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    DomainTemplateServiceImpl domainTemplateService

    @Autowired
    DomainStateMapper domainStateMapper

    def "create specific domain from template"() {
        given: "a client"
        Domain domainFromTemplate = null
        Client client = null
        txTemplate.execute {
            client = repository.save(newClient { })
            domainFromTemplate = createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.id == DSGVO_TEST_DOMAIN_TEMPLATE_ID
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            templateVersion == domainTemplate.templateVersion
        }
        domainFromTemplate.catalogItems.size() == 6
        with (domainFromTemplate.catalogItems.sort { it.name }) {
            it[0].tailoringReferences.size()==1
            it[0].tailoringReferences.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
            it[0].tailoringReferences.first().target == it[5]
            with(it[0]) {
                name == 'Control-1'
                abbreviation == 'c-1'
                description.startsWith('Lore')
                elementType == 'control'
                //                getRiskValues(domainFromTemplate).present
                // TODO: VEO-2285
            }

            it[1].tailoringReferences.size() == 0
            with(it[1]) {
                name == 'Control-2'
                abbreviation == 'c-2'
                //                getRiskValues(domainFromTemplate).present
            }

            it[2].tailoringReferences.size()==1
            it[2].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[2].tailoringReferences.first().target == it[0]
            with(it[2]) {
                name == 'Control-3'
                abbreviation == 'c-3'
                //                getRiskValues(domainFromTemplate).present
            }

            with(it[3]) {
                name == 'Control-cc-1'
                //                getRiskValues(domainFromTemplate).present
            }

            with(it[4]) {
                name == 'Control-cc-2'
                //                getRiskValues(domainFromTemplate).present
            }

            with(it[5]) {
                name == 'Test process-1'
                //                getImpactValues(domainFromTemplate).present
            }
        }
    }

    def "create a domainTemplate from domain"() {
        given: "a client"
        Client client = repository.save(newClient { })

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
            domainFromTemplate = domainTemplateService.createDomain(client, domainTemplateFromDomain.id)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
            // initialize lazy association
            client.domains*.domainTemplate*.name
        }
        domainFromTemplate = client.domains.find { it.domainTemplate.id == domainTemplateFromDomain.id }

        expect: 'the domain matches'
        domainFromTemplate.domainTemplate.id == domainTemplateFromDomain.id
        domainFromTemplate.templateVersion == "1.1.0"
        domainFromTemplate.catalogItems.size() == 6
        with (domainFromTemplate.catalogItems.sort { it.name }) {
            it[0].name == 'Control-1'
            it[0].abbreviation == 'c-1'
            it[0].description.startsWith('Lore')
            it[0].tailoringReferences.size()==1
            it[0].tailoringReferences.first().referenceType == TailoringReferenceType.LINK_EXTERNAL
            it[0].tailoringReferences.first().target == it[5]

            it[1].name == 'Control-2'
            it[1].abbreviation == 'c-2'
            it[1].tailoringReferences.size() == 0

            it[2].name == 'Control-3'
            it[2].abbreviation == 'c-3'
            it[2].tailoringReferences.size()==1
            it[2].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[2].tailoringReferences.first().target == it[0]

            it[3].name == 'Control-cc-1'

            it[4].name == 'Control-cc-2'
            it[4].subType == "CTL_TOM"

            it[5].name == 'Test process-1'
        }
    }

    def "create a domainTemplate from domain twice"() {
        given: "a client"
        Client client = repository.save(newClient {})

        def domainFromTemplate = null
        txTemplate.execute {
            createTestDomainTemplate(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            domainFromTemplate = domainTemplateService.createDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()
        domainFromTemplate.templateVersion = "1.0.1"

        txTemplate.execute {
            domainTemplateService.createDomainTemplateFromDomain(domainFromTemplate)
        }

        when:"created with same name/version"
        txTemplate.execute {
            domainTemplateService.createDomainTemplateFromDomain(domainFromTemplate)
        }

        then: "an exception is thrown"
        thrown(ModelConsistencyException)
    }

    def "create a domain whose catalog contains a composite"() {
        given: "a client"
        Client client = repository.save(newClient { })

        def domainFromTemplate = null
        txTemplate.execute {
            def testDomainVeo612TemplateId = UUID.fromString("40df37d5-cd23-528e-bfc2-7b73c0aa2c67")
            createTestDomainTemplate(testDomainVeo612TemplateId)
            domainFromTemplate = domainTemplateService.createDomain(client, testDomainVeo612TemplateId)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.idAsString == "40df37d5-cd23-528e-bfc2-7b73c0aa2c67"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            templateVersion == domainTemplate.templateVersion
        }
        domainFromTemplate.catalogItems.size() == 3

        with (domainFromTemplate.catalogItems.sort { it.name }) {
            it[0].name == 'All controls'
            it[1].name == 'Control-1'
            it[2].name == 'Control-2'
            it[0].tailoringReferences.collect {it.target.name}.toSorted() == ['Control-1', 'Control-2']
        }
    }

    def "create a domain whose catalog contains a scope"() {
        given: "a client"
        Client client = repository.save(newClient { })

        def domainFromTemplate = null
        txTemplate.execute {
            def testDomainTemplateVeo620 = UUID.fromString("99af3dc3-eb17-5267-afc5-14e9f2ebd701")
            createTestDomainTemplate(testDomainTemplateVeo620)
            domainFromTemplate = domainTemplateService.createDomain(client, testDomainTemplateVeo620)
            client.addToDomains(domainFromTemplate)
            client = repository.save(client)
        }
        domainFromTemplate = client.domains.first()

        expect: 'the domain matches'
        domainFromTemplate != null
        with (domainFromTemplate) {
            domainTemplate.idAsString == "99af3dc3-eb17-5267-afc5-14e9f2ebd701"
            name == domainTemplate.name
            abbreviation == domainTemplate.abbreviation
            description == domainTemplate.description
            authority == domainTemplate.authority
            templateVersion == domainTemplate.templateVersion
        }
        domainFromTemplate.catalogItems.size() == 3
        with (domainFromTemplate.catalogItems.sort { it.name }) {
            it[0].name == 'Asset 1'
            it[0].tailoringReferences[0].target.name == "Scope 1"
            it[0].tailoringReferences[0].referenceType == TailoringReferenceType.SCOPE
            it[0].tailoringReferences.size() == 1
            it[1].name == 'Asset 2'
            it[1].tailoringReferences[0].referenceType == TailoringReferenceType.SCOPE
            it[1].tailoringReferences.size() == 1
            it[2].name == 'Scope 1'
            it[2].tailoringReferences.collect {it.target.name}.toSorted() == ['Asset 1', 'Asset 2']
            it[2].tailoringReferences[0].referenceType == TailoringReferenceType.MEMBER
            it[2].tailoringReferences[1].referenceType == TailoringReferenceType.MEMBER
        }
    }

    def "copy a profile with multiple references to the same catalog item"() {
        given:
        def template = newDomainTemplate() {t->
            catalogItems = [
                newCatalogItem(t) {
                    elementType = 'scenario'
                }
            ]
            profiles = [
                newProfile(t) {
                    id = UUID.randomUUID()
                    items = [
                        newProfileItem(it) {
                            elementType = 'scenario'
                            appliedCatalogItem = t.catalogItems.first()
                        },
                        newProfileItem(it) {
                            elementType = 'scenario'
                            appliedCatalogItem = t.catalogItems.first()
                        }
                    ]
                }
            ]
        }

        def profile = template.profiles.first()
        def client = newClient()
        def domain = domainStateMapper.toDomain(template, false)

        when:
        domainTemplateService.copyProfileToDomain(profile, domain)

        then:
        domain.profiles.size() ==1
        domain.catalogItems.size() == 1
        with(domain.profiles.first()) {
            items.size() == 2
            items*.appliedCatalogItem == [
                domain.catalogItems.first(),
                domain.catalogItems.first()
            ]
        }
    }
}