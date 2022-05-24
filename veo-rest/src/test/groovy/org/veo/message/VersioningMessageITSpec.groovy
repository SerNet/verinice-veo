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
package org.veo.message

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Key
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PersonRepository
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.persistence.access.StoredEventRepository
import org.veo.rest.configuration.WebMvcSecurityConfiguration

class VersioningMessageITSpec extends VeoSpringSpec {
    private clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    @Autowired
    CreateUnitUseCase createUnitUseCase;

    @Autowired
    StoredEventRepository storedEventRepository

    @Autowired
    private ClientRepository clientRepository

    @Autowired
    PersonRepository personRepository

    @WithUserDetails("user@domain.example")
    def "creation messages produced for client creation with demo unit"() {
        when: "creating a client with a demo unit"
        executeInTransaction {
            createUnitUseCase.execute(new CreateUnitUseCase.InputData(
                    new NameableInputData(Optional.empty(), "non-demo-unit", "ndu", "whatever"),
                    clientId, Optional.empty()
                    ))
        }

        and: "fetching all messages"
        def messages = storedEventRepository.findAll()
                .findAll { it.routingKey.contains("versioning_event") }
                .collect { new ObjectMapper().readValue(it.content, Map.class) }

        then: "there is one creation message for each person"
        def persons = personRepository.query(clientRepository.findById(clientId).get()).execute(PagingConfiguration.UNPAGED).resultPage
        persons.size() > 0
        persons.forEach({ person ->
            def elementMessages = messages.findAll { it.uri?.contains("/persons/${person.idAsString}") }
            assert elementMessages.size() == 1
            elementMessages.first().with{
                assert type == "CREATION"
                assert changeNumber == 0
                assert content.designator.contains("DMO-")
            }
        })

        and: "there is one creation message for each catalog item"
        var catalogItems = txTemplate.execute {
            domainDataRepository.findAllByClient(clientId.uuidValue())
                    .collectMany { it.catalogs }
                    .collectMany { it.catalogItems }
        }
        catalogItems.size() > 0
        catalogItems.forEach({ item ->
            def itemMessages = messages.findAll { it.uri?.contains("/items/$item.idAsString") }
            assert itemMessages.size() == 2
            assert itemMessages.find{it.type == "CREATION"}.changeNumber == 0
            assert itemMessages.find{it.type == "MODIFICATION"}.changeNumber == 1
        })
    }
}
