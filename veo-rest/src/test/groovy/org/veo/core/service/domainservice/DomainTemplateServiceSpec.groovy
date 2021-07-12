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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.service.DomainTemplateService
import org.veo.persistence.access.ClientRepositoryImpl

@SpringBootTest(classes = DomainTemplateServiceSpec.class)
@ComponentScan("org.veo")
@ActiveProfiles(["test"])
class DomainTemplateServiceSpec extends VeoSpringSpec {
    @Autowired
    private ClientRepositoryImpl repository

    @Autowired
    DomainTemplateServiceImpl domainTemplateService

    def "create default domain from template"() {
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
        def domainFromTemplate = client.domains.first()
        expect: 'the domain matches with the linked domainTemplate'
        domainFromTemplate.domainTemplate.dbId == DomainTemplateService.DSGVO_DOMAINTEMPLATE_UUID
        domainFromTemplate.name == domainFromTemplate.domainTemplate.name
        domainFromTemplate.abbreviation == domainFromTemplate.domainTemplate.abbreviation
        domainFromTemplate.description == domainFromTemplate.domainTemplate.description
        domainFromTemplate.authority == domainFromTemplate.domainTemplate.authority
        domainFromTemplate.revision == domainFromTemplate.domainTemplate.revision
        domainFromTemplate.templateVersion == domainFromTemplate.domainTemplate.templateVersion

        domainFromTemplate.catalogs.size() == 1
        domainFromTemplate.catalogs.first().catalogItems.size() == 5
        with (domainFromTemplate.catalogs.first().catalogItems.sort { it.element.name }) {
            it[0].tailoringReferences.size()==0
            it[0].element.abbreviation == 'c-1'
            it[0].element.name == 'Control-1'
            it[0].element.description.startsWith('Lore')

            it[1].element.abbreviation == 'c-2'

            it[2].element.abbreviation == 'c-3'
            it[2].element.links.size()==1
            it[2].element.links.first().target == it[0].element
            it[2].tailoringReferences.size()==1
            it[2].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[2].tailoringReferences.first().catalogItem == it[0]

            it[3].element.abbreviation == 'cc-1'
            it[3].element.links.size()==1
            it[3].element.links.first().target == it[4].element
            it[3].tailoringReferences.size()==1
            it[3].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[3].tailoringReferences.first().catalogItem == it[4]

            it[4].element.abbreviation == 'cc-2'
            it[4].element.links.size()==1
            it[4].element.links.first().target == it[3].element
            it[4].tailoringReferences.size()==1
            it[4].tailoringReferences.first().referenceType == TailoringReferenceType.LINK
            it[4].tailoringReferences.first().catalogItem == it[3]
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
        domainFromTemplate.domainTemplate.dbId == "00000000-0000-0000-0000-000000000001"
        domainFromTemplate.name == domainFromTemplate.domainTemplate.name
        domainFromTemplate.abbreviation == domainFromTemplate.domainTemplate.abbreviation
        domainFromTemplate.description == domainFromTemplate.domainTemplate.description
        domainFromTemplate.authority == domainFromTemplate.domainTemplate.authority
        domainFromTemplate.revision == domainFromTemplate.domainTemplate.revision
        domainFromTemplate.templateVersion == domainFromTemplate.domainTemplate.templateVersion

        domainFromTemplate.catalogs.size() == 1
        domainFromTemplate.catalogs.first().name == 'DVGO-Controls'
        domainFromTemplate.catalogs.first().catalogItems.size() == 5
    }
}
