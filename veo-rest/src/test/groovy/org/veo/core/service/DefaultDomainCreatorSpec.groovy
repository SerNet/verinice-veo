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
package org.veo.core.service

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.repository.DomainTemplateRepository
import org.veo.service.DefaultDomainCreator

import spock.lang.Specification

class DefaultDomainCreatorSpec extends Specification {

    DomainTemplateService domainTemplateService = Mock()
    DomainTemplateRepository domainTemplateRepo = Mock()
    DefaultDomainCreator defaultDomainCreator = new DefaultDomainCreator(Set.of("DS-GVO", "ISO"), domainTemplateService, domainTemplateRepo)

    def "adds default domains"() {
        given: 'a client, one DS-GVO template and an ISO template'
        def client = Mock(Client) {
            domains >> []
        }

        def dsgvoTemplateId = Key.newUuid()
        def dsgvoDomain = Mock(Domain)
        def isoTemplateId = Key.newUuid()
        def isoDomain = Mock(Domain)

        when: 'default domains are created'
        defaultDomainCreator.addDefaultDomains(client)

        then: 'both templates are incarnated in the client'
        1 * domainTemplateRepo.getLatestDomainTemplateId("DS-GVO") >> Optional.of(dsgvoTemplateId)
        1 * domainTemplateRepo.getLatestDomainTemplateId("ISO") >> Optional.of(isoTemplateId)
        1 * domainTemplateService.createDomain(client, dsgvoTemplateId.uuidValue()) >> dsgvoDomain
        1 * domainTemplateService.createDomain(client, isoTemplateId.uuidValue()) >> isoDomain
        1 * client.addToDomains(dsgvoDomain)
        1 * client.addToDomains(isoDomain)
    }

    def "ignores missing domain template"() {
        given: "a client and an ISO template"
        def client = Mock(Client) {
            domains >> []
        }

        def isoTemplateId = Key.newUuid()
        def isoDomain = Mock(Domain)

        when: 'default domains are created'
        defaultDomainCreator.addDefaultDomains(client)

        then: 'only the present domain template is incarnated'
        1 * domainTemplateRepo.getLatestDomainTemplateId("DS-GVO") >> Optional.empty()
        1 * domainTemplateRepo.getLatestDomainTemplateId("ISO") >> Optional.of(isoTemplateId)
        1 * domainTemplateService.createDomain(client, isoTemplateId.uuidValue()) >> isoDomain
        1 * client.addToDomains(isoDomain)
    }

    def "don't create domain when client has domain"() {
        given: "a client with a domain"
        def client = Mock(Client) {
            domains >> [Mock(Domain)]
        }

        when: 'default domains are created'
        defaultDomainCreator.addDefaultDomains(client)

        then: 'an exception is thrown'
        thrown(IllegalArgumentException)
    }
}
