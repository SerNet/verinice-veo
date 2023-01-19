/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Client
import org.veo.core.usecase.domain.CreateDomainUseCase
import org.veo.core.usecase.domaintemplate.EntityAlreadyExistsException
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

@WithUserDetails("content-creator")
class CreateDomainUseCaseITSpec extends VeoSpringSpec{
    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    CreateDomainUseCase useCase

    Client client

    def setup() {
        client = createTestClient()
    }

    def "adds new domain"() {
        given: "an existing domain and a template"
        domainTemplateRepository.save(newDomainTemplate {
            name = "ISO"
        })
        domainRepository.save(newDomain(client) {
            name = "DS-GVO"
        })

        when: "creating a domain"
        useCase.execute(new CreateDomainUseCase.InputData(client, "do-main", "dom", "it's great", "st. nic"))

        then: "it has been persisted"
        def domain = domainRepository
                .findAllActiveByClient(client.idAsString)
                .find { it.name == "do-main" }
        domain.abbreviation == "dom"
        domain.description == "it's great"
        domain.authority == "st. nic"
    }

    def "fails if name is occupied by template"() {
        given: "an existing template"
        domainTemplateRepository.save(newDomainTemplate {
            name = "do-main"
        })

        when: "attempting to create a domain with the same name"
        useCase.execute(new CreateDomainUseCase.InputData(client, "do-main", "dom", "it's great", "st. nic"))

        then: "it fails"
        def ex = thrown(EntityAlreadyExistsException)
        ex.message == "Templates already exist for domain name 'do-main'"
    }

    def "fails if name is occupied by other domain in client"() {
        given: "an existing domain"
        domainRepository.save(newDomain(client) {
            name = "do-main"
        })

        when: "attempting to create a domain with the same name"
        useCase.execute(new CreateDomainUseCase.InputData(client, "do-main", "dom", "it's great", "st. nic"))

        then: "it fails"
        def ex = thrown(EntityAlreadyExistsException)
        ex.message == "A domain with name 'do-main' already exists in this client"
    }
}
