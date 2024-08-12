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
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.persistence.access.jpa.StoredEventDataRepository

class VersioningMessageITSpec extends VeoSpringSpec {
    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
    }

    @Autowired
    GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase

    @Autowired
    ApplyProfileIncarnationDescriptionUseCase applyProfileIncarnationDescriptionUseCase

    @Autowired
    StoredEventDataRepository storedEventRepository

    @Autowired
    private ClientRepository clientRepository

    @Autowired
    PersonRepository personRepository

    @Autowired
    ProcessRepository processRepository

    @Autowired
    ScopeRepository scopeRepository

    @WithUserDetails("user@domain.example")
    def "creation messages produced when applying profile"() {
        when: "creating a client and applying example profile"
        def client = createTestClient()
        executeInTransaction {
            defaultDomainCreator.addDomain(client,"DS-GVO", true)
            client = clientRepository.save(client)
            var dsgvo = client.domains.find { it.name == "DS-GVO" }
            def unit = unitDataRepository.save(newUnit(client))
            def profileId = dsgvo.profiles.find { it.name == "Beispielorganisation" }.id
            var incarnationDescriptions = getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(client, unit.id, dsgvo.id, null, profileId, false)
                    ).references
            applyProfileIncarnationDescriptionUseCase.execute(
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(client, unit.id, incarnationDescriptions)
                    )
        }

        and: "fetching all messages"
        def messages = storedEventRepository.findAll()
                .findAll { it.routingKey.contains("entity_revision") }
                .collect { new ObjectMapper().readValue(it.content, Map.class) }

        then: "no message with uri null"
        messages.every {it.uri != null}

        and: "there is one creation message for each person"
        def persons = personRepository.query(client).execute(PagingConfiguration.UNPAGED).resultPage
        persons.size() > 0
        persons.forEach({ person ->
            def elementMessages = messages.findAll { it.uri?.endsWith("/persons/${person.idAsString}") }
            assert elementMessages.size() == 1
            elementMessages.first().with{
                assert type == "CREATION"
                assert changeNumber == 0
                assert content.designator.contains("PER-")
            }
        })

        and: "there are no searches or resources uris in the content"
        with(messages.find{it.type != 'HARD_DELETION' && it.uri?.contains('/persons/')}) {
            with(content.owner) {
                it.containsKey 'targetUri'
                !it.containsKey('searchesUri')
                !it.containsKey('resourcesUri')
            }
        }

        and: "there is one creation message for each process"
        def processes = processRepository.query(client).execute(PagingConfiguration.UNPAGED).resultPage
        processes.size() > 0
        processes.forEach({ process ->
            def elementMessages = messages.findAll { it.uri?.endsWith("/processes/${process.idAsString}") }
            assert elementMessages.size() == 1
            with(elementMessages.first()) {
                type == "CREATION"
                eventType == "entity_revision"
                changeNumber == 0
                content.designator.contains("PRO-")
                time != null
            }
        })

        and: "there is one creation message for each scope"
        def scopes = scopeRepository.query(client).execute(PagingConfiguration.UNPAGED).resultPage
        scopes.size() > 0
        scopes.forEach({ scope ->
            def elementMessages = messages.findAll { it.uri?.endsWith("/scopes/${scope.idAsString}") }
            assert elementMessages.size() == 1
            elementMessages.first().with{
                eventType == "entity_revision"
                assert type == "CREATION"
                assert changeNumber == 0
                assert content.designator.contains("SCP-")
            }
        })

        and: "there are no creation messages for the content"
        def contentMessages = messages.findAll { it.uri?.contains("/domains/") }
        assert contentMessages.size() == 0
    }
}
