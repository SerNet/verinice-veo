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
import org.veo.core.entity.groups.PersonGroup
import org.veo.core.entity.groups.Scope
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.EntityGroupDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class GroupJpaSpec extends AbstractJpaSpec {
    @Autowired
    EntityGroupDataRepository entityGroupDataRepository

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
            entityGroupDataRepository.save(group1).id.uuidValue()
        }
        then: "the entities can be retrieved"
        def retrievedGroup1 = (AssetGroup)entityGroupDataRepository.findById(group1Id).get()
        def retrievedGroup2 = (AssetGroup)retrievedGroup1.members.first()
        and: "group 1 is always the same object"
        assertSame(retrievedGroup2.members.first(), retrievedGroup1)
    }


    @Transactional
    def "save and load a scope"() {
        when:"saving a scope with subgroups and elements"
        def client = newClient()
        def unit = newUnit(client)
        def asset = newAsset(unit)
        def assetGroup = newAssetGroup(unit) {
            members = [asset]
        }
        def person = newPerson(unit)
        def personGroup = newPersonGroup(unit) {
            members = [person]
        }
        def scope = newScope(unit) {
            members = [assetGroup, personGroup]
        }
        def scopeId = transactionTemplate.execute {
            clientRepository.save(client)
            unitRepository.save(unit)
            entityGroupDataRepository.save(scope).id.uuidValue()
        }
        then: "the scope can be retrieved"
        def retrievedScope = (Scope) entityGroupDataRepository.findById(scopeId).get()
        and: "it contains the groups"
        retrievedScope.members.size() == 2
        retrievedScope.members.contains assetGroup
        retrievedScope.members.contains personGroup
        and: "the groups contain the elements"
        def retrievedAssetGroup = retrievedScope.members.findAll {it instanceof AssetGroup}.first()
        def retrievedPersonGroup = retrievedScope.members.findAll {it instanceof PersonGroup}.first()
        retrievedAssetGroup.members == [asset] as Set
        retrievedPersonGroup.members == [person] as Set
    }
}
