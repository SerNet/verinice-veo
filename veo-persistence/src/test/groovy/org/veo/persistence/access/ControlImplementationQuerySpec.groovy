/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Aziz Khalledi
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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mapping.PropertyReferenceException

import org.veo.core.entity.Asset
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.repository.ControlImplementationQuery
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PagingConfiguration.SortOrder
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.ControlImplementationDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.access.query.ControlImplementationQueryImpl
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ClientData

class ControlImplementationQuerySpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientDataRepository
    @Autowired
    ControlImplementationDataRepository controlImplementationRepository
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ControlDataRepository controlDataRepository
    @Autowired
    AssetDataRepository assetDataRepository
    @Autowired
    ScopeDataRepository scopeDataRepository
    @Autowired
    PersonDataRepository personDataRepository

    ClientData client
    Domain domain
    Unit unit
    Asset asset

    ControlImplementationQuery query

    def setup() {
        client = clientDataRepository.save(newClient {
            id = UUID.randomUUID()
            newDomain(it)
        })
        unit = unitRepository.save(newUnit(client))
        domain = client.domains.first()
        def control1 =
                controlDataRepository.save(newControl(unit) {
                    name = "Control 1"
                    abbreviation = "c1"
                })
        control1.associateWithDomain(domain, "control", "NEW")

        def person1 = personDataRepository.save(newPerson(unit).tap {
            name = 'Jane Doe'
        })
        def person2 = personDataRepository.save(newPerson(unit).tap {
            name = 'John Doe'
        })
        def person3 = personDataRepository.save(newPerson(unit).tap {
            name = 'Mary Doe'
        })

        asset = assetDataRepository.save(newAsset(unit).tap {
            implementControl(control1).tap {
                responsible = person1
            }
            abbreviation = "ABB1"
            associateWithDomain(domain, "asset", "NEW")
        })

        scopeDataRepository.save(newScope(unit).tap {
            implementControl(control1).tap {
                responsible = person2
            }
            abbreviation = "ABB3"
            associateWithDomain(domain, "scope", "NEW")
        })
        scopeDataRepository.save(newScope(unit).tap {
            implementControl(control1)
                    .tap {
                        responsible = person3
                    }
            abbreviation = "ABB2"
            associateWithDomain(domain, "scope", "NEW")
        })

        query = new ControlImplementationQueryImpl(controlImplementationRepository, client, domain.getId())
    }

    def 'sort CIs by Risk Affected abbreviation'() {
        when:
        def result = query.execute(new PagingConfiguration<>(Integer.MAX_VALUE, 0, "owner.abbreviation", SortOrder.DESCENDING))

        then:
        result.totalResults == 3
        result.resultPage*.owner*.abbreviation == ["ABB3", "ABB2", "ABB1"]

        when: 'sorting with an invalid parameter results in an error'
        result = query.execute(new PagingConfiguration(Integer.MAX_VALUE, 0, "owner.nonExistingProp", SortOrder.DESCENDING))

        then:
        thrown(PropertyReferenceException)
    }

    def 'filter CIs by risk-affected'() {
        when:
        query.whereRiskAffectedIs(asset.id)
        def result = query.execute(new PagingConfiguration(Integer.MAX_VALUE, 0, "control.name", SortOrder.ASCENDING))

        then:
        result.totalResults == 1
        result.resultPage*.owner*.abbreviation == ["ABB1"]
    }

    def 'sort CIs by responsible name'() {
        when:
        def result = query.execute(new PagingConfiguration(Integer.MAX_VALUE, 0, "responsible.name", SortOrder.ASCENDING))

        then:
        result.totalResults == 3
        result.resultPage*.owner*.abbreviation == ["ABB1", "ABB3", "ABB2"]
    }
}