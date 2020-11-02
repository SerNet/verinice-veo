/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa

import static org.junit.Assert.assertSame

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.groups.AssetGroup
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class GroupJpaSpec extends AbstractJpaSpec {
    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    TransactionTemplate transactionTemplate

    @Transactional
    def "circular groups are supported"() {
        when:"saving a circular asset group structure"
        def group1Id = transactionTemplate.execute {
            def client = newClient()
            def unit = newUnit(client)
            def group1 = newAssetGroup(unit)
            def group2 = newAssetGroup(unit) {
                members = [group1]
            }
            group1.members.add(group2)
            clientRepository.save(client)
            unitRepository.save(unit)
            assetRepository.save(group1).id.uuidValue()
        }
        then: "the entities can be retrieved"
        def retrievedGroup1 = (AssetGroup)assetRepository.findById(group1Id).get()
        def retrievedGroup2 = (AssetGroup)retrievedGroup1.members.first()
        and: "group 1 is always the same object"
        assertSame(retrievedGroup2.members.first(), retrievedGroup1)
    }
}
