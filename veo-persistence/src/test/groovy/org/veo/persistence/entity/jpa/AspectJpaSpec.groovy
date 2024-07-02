/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.persistence.entity.jpa

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class AspectJpaSpec extends AbstractJpaSpec {
    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    UnitDataRepository unitRepository

    Domain domain0
    Domain domain1
    Unit unit

    def setup() {
        def client = clientDataRepository.save(newClient())
        unit = newUnit(client)
        unit = unitRepository.save(unit)

        domain0 = domainRepository.save(newDomain(client))
        domain1 = domainRepository.save(newDomain(client))
        client.domains = [domain0, domain1]
        clientDataRepository.save(client)
    }

    def 'aspect is inserted'() {
        given: "an asset with a sub type aspect"
        def asset = newAsset(unit) {
            associateWithDomain(domain0, "foo", "NEW")
        }

        when: "saving and retrieving the asset"
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.dbId)

        then: "the aspect exists"
        with(retrievedAsset.get().domainAssociations) {
            size() == 1
            it[0].subType == "foo"
        }
    }

    def 'aspect value can be changed'() {
        given: "a saved asset with two sub types for domains 0 & 1"
        def asset = newAsset(unit) {
            associateWithDomain(domain0, "foo", "NEW")
            associateWithDomain(domain1, "bar", "NEW")
        }
        assetRepository.save(asset)

        when: "changing the status for domain 1, saving & retrieving"
        asset.setStatus("OLD", domain1)
        assetRepository.save(asset)
        def retrievedAsset = assetRepository.findById(asset.dbId)

        then: "the new status has been applied"
        with(retrievedAsset.get().domainAssociations.sort { it.subType }) {
            size() == 2
            it[0].subType == "bar"
            it[0].status == "OLD"
            it[0].domain == domain1
            it[1].subType == "foo"
            it[1].status == "NEW"
            it[1].domain == domain0
        }
    }
}
