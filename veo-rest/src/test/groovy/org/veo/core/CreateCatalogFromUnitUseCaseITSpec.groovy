/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.domain.CreateCatalogFromUnitUseCase
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.rest.security.NoRestrictionAccesRight

@WithUserDetails("content-creator")
class CreateCatalogFromUnitUseCaseITSpec extends VeoSpringSpec{
    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    AssetRepository assetRepository

    @Autowired
    CreateCatalogFromUnitUseCase useCase

    Client client

    def setup() {
        client = createTestClient()
    }

    def "update a catalog item with a parent"() {
        given:
        def client = createTestClient()
        def domain = domainRepository.save(newDomain(client))
        def unit = unitRepository.save(newUnit(client))
        def asset = assetRepository.save(newAsset(unit) {
            associateWithDomain(domain, "Asset", "good")
        })

        when:
        executeInTransaction {
            useCase.execute(new CreateCatalogFromUnitUseCase.InputData(domain.id, client, unit.id))
        }
        domain = executeInTransaction {
            domainRepository.findById(domain.id).get().tap {
                it.catalogItems.size()
            }
        }

        then:
        domain.catalogItems.size() == 1

        when:
        executeInTransaction {
            def parent = assetRepository.save(newAsset(unit) {
                associateWithDomain(domain, "Asset", "even better")
            })

            assetRepository.getById(asset.id, NoRestrictionAccesRight.from(client.idAsString)).tap {
                parent.parts.add(it)
            }
        }
        executeInTransaction {
            useCase.execute(new CreateCatalogFromUnitUseCase.InputData(domain.id, client, unit.id))
        }
        domain = executeInTransaction {
            domainRepository.findById(domain.id).get().tap {
                it.catalogItems.each {
                    it.tailoringReferences.size()
                }
            }
        }

        then:
        with(domain.catalogItems) {
            it.size() == 2
        }
    }
}
