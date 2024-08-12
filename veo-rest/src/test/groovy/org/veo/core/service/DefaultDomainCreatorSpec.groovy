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
    DefaultDomainCreator defaultDomainCreator = new DefaultDomainCreator(domainTemplateService, domainTemplateRepo)

    def "adds default domains"() {
        given: 'a client, one DS-GVO template and an ISO template'
        def client = Mock(Client) {
            domains >> []
        }

        def dsgvoTemplateId = Key.newUuid()
        def dsgvoDomain = Mock(Domain)
        def isoTemplateId = Key.newUuid()
        def isoDomain = Mock(Domain)

        when: 'domains are created'
        defaultDomainCreator.addDomain(client,"DS-GVO", true)
        defaultDomainCreator.addDomain(client,"ISO", false)

        then: 'both templates are incarnated in the client'
        1 * domainTemplateRepo.getLatestDomainTemplateId("DS-GVO") >> Optional.of(dsgvoTemplateId)
        1 * domainTemplateRepo.getLatestDomainTemplateId("ISO") >> Optional.of(isoTemplateId)
        1 * domainTemplateService.createDomain(client, dsgvoTemplateId.uuidValue(), true) >> dsgvoDomain
        1 * domainTemplateService.createDomain(client, isoTemplateId.uuidValue(), false) >> isoDomain
        1 * client.addToDomains(dsgvoDomain)
        1 * client.addToDomains(isoDomain)
    }
}
