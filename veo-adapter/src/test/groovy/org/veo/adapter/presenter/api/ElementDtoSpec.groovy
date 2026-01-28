/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.adapter.presenter.api

import org.veo.adapter.presenter.api.common.DomainBaseIdRef
import org.veo.adapter.presenter.api.dto.AssetDomainAssociationDto
import org.veo.adapter.presenter.api.dto.CustomAspectDto
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.core.entity.Domain

import spock.lang.Specification

class ElementDtoSpec extends Specification {

    def "Extract DomainAssociatonStates for multi-domain element"() {
        given:
        def dto = new FullAssetDto()
        def domain1Id = UUID.randomUUID()
        def domain2Id = UUID.randomUUID()
        DomainBaseIdRef<Domain> domain1Ref = Stub {
            getId() >> domain1Id
        }
        DomainBaseIdRef<Domain> domain2Ref = Stub {
            getId() >> domain2Id
        }
        def domain1CA = new CustomAspectDto().tap {
            setDomains([domain1Ref] as Set)
        }
        def domain2CA = new CustomAspectDto().tap {
            setDomains([domain2Ref] as Set)
        }
        def allDomainsCA = new CustomAspectDto()
                .tap {
                    setDomains([domain1Ref, domain2Ref] as Set)
                }
        def noDomainsCA = new CustomAspectDto()

        def domain1Link = new CustomLinkDto().tap {
            setDomains([domain1Ref] as Set)
        }
        def domain2Link = new CustomLinkDto().tap {
            setDomains([domain2Ref] as Set)
        }
        def allDomainsLink = new CustomLinkDto().tap {
            setDomains([domain1Ref, domain2Ref] as Set)
        }
        def noDomainsLink = new CustomLinkDto()

        dto.setDomains([(domain1Id): new AssetDomainAssociationDto(), (domain2Id): new AssetDomainAssociationDto()])
        dto.setCustomAspects(['d1ca': domain1CA, 'd2ca': domain2CA, 'all': allDomainsCA, no: noDomainsCA])
        dto.setLinks(['d1l': [domain1Link], 'd2l': [domain2Link], 'all': [allDomainsLink], no: [noDomainsLink]])

        when:
        def associations = dto.getDomainAssociationStates()

        then:
        associations.size() == 2
        with(associations.find { it.domain.id == domain1Id }) {
            it.customAspectStates.size() == 3
            it.customAspectStates*.type ==~ ['d1ca', 'all', 'no']
            it.customLinkStates.size() == 3
            it.customLinkStates*.type ==~ ['d1l', 'all', 'no']
        }
        with(associations.find { it.domain.id == domain2Id }) {
            it.customAspectStates.size() == 3
            it.customAspectStates*.type ==~ ['d2ca', 'all', 'no']
            it.customLinkStates.size() == 3
            it.customLinkStates*.type ==~ ['d2l', 'all', 'no']
        }
    }
}