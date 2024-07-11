/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler.
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import org.veo.core.entity.Client
import org.veo.core.entity.Unit
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase
import org.veo.core.usecase.unit.DeleteUnitUseCase.InputData
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.metrics.DataSourceProxyBeanPostProcessor

import net.ttddyy.dsproxy.QueryCountHolder

@WithUserDetails("user@domain.example")
class GetProfileIncarnationUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase

    @Autowired
    private UnitRepository unitRepository

    def "getProfileIncarnationDescriptionUseCase not compact"() {
        given: 'a unit with example elements'
        def client = createTestClient()
        var domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.getById(client.id)
        def unit = unitRepository.save(newUnit(client))

        when:
        def result = executeInTransaction {
            def profileId = domain.profiles.first().id
            getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(client, unit.id, domain.id, null, profileId,false)
                    ).references
        }

        then: 'all tailoring references are returned'
        result.collectMany{it.references}.size() == 23

        when: "we get only the distinct tailoring references"
        result = executeInTransaction {
            def profileId = domain.profiles.first().id
            getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(client, unit.id, domain.id, null, profileId, true)
                    ).references
        }

        then: 'less tailoring references are returned'
        result.collectMany{it.references}.size() == 12
    }
}