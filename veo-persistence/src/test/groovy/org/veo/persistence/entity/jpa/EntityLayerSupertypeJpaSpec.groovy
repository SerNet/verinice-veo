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

import org.springframework.beans.factory.annotation.Autowired

import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.groups.AssetGroupData

class EntityLayerSupertypeJpaSpec extends AbstractJpaSpec {

    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    UnitData owner0;
    UnitData owner1;
    UnitData owner2;

    def setup() {
        owner0 = unitRepository.save(new UnitData(
                dbId: UUID.randomUUID().toString(),
                name: "owner 0"
                ))
        owner1 = unitRepository.save(new UnitData(
                dbId: UUID.randomUUID().toString(),
                name: "owner 1"
                ))
        owner2 = unitRepository.save(new UnitData(
                dbId: UUID.randomUUID().toString(),
                name: "owner 2"
                ))
    }

    def 'finds entities by owners'() {
        given: "three assets from different owners"

        assetRepository.save(new AssetData(
                dbId: UUID.randomUUID().toString(),
                name: "asset 0",
                owner: owner0
                ))
        assetRepository.save(new AssetData(
                dbId: UUID.randomUUID().toString(),
                name: "asset 1",
                owner: owner1
                ))
        assetRepository.save(new AssetData(
                dbId: UUID.randomUUID().toString(),
                name: "asset 2",
                owner: owner2
                ))

        when: "querying assets from the first two owners"
        def result = assetRepository.findByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' assets are returned"
        with(result.sort {it.name}) {
            size() == 2
            it[0].name == "asset 0"
            it[1].name == "asset 1"
        }
    }

    def 'finding entities by owners excludes groups'() {
        given: "a normal asset and an asset group, both from the same owner"
        assetRepository.save(new AssetData(
                dbId: UUID.randomUUID().toString(),
                name: "normal",
                owner: owner0
                ))
        assetRepository.save(new AssetGroupData(
                dbId: UUID.randomUUID().toString(),
                name: "group",
                owner: owner0
                ))

        when: "querying the owner's assets"
        def result = assetRepository.findByUnits([owner0.dbId] as Set)

        then: "only the normal asset is returned"
        result.size() == 1
        result[0].name == "normal"
    }

    def 'finds entity groups by owners'() {
        given: "three asset groups from different owners"
        assetRepository.save(new AssetGroupData(
                dbId: UUID.randomUUID().toString(),
                name: "group 0",
                owner: owner0
                ))
        assetRepository.save(new AssetGroupData(
                dbId: UUID.randomUUID().toString(),
                name: "group 1",
                owner: owner1
                ))
        assetRepository.save(new AssetGroupData(
                dbId: UUID.randomUUID().toString(),
                name: "group 2",
                owner: owner2
                ))

        when: "querying groups from the first two owners"
        def result = assetRepository.findGroupsByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' assets are returned"
        with(result.sort {it.name}) {
            size() == 2
            it[0].name == "group 0"
            it[1].name == "group 1"
        }
    }

    def 'finding entity groups by owners excludes normal entities'() {
        given: "a normal asset and an asset group, both from the same owner"
        assetRepository.save(new AssetData(
                dbId: UUID.randomUUID().toString(),
                name: "normal",
                owner: owner0
                ))
        assetRepository.save(new AssetGroupData(
                dbId: UUID.randomUUID().toString(),
                name: "group",
                owner: owner0
                ))

        when: "querying the owner's asset groups"
        def result = assetRepository.findGroupsByUnits([owner0.dbId] as Set)

        then: "only the group is returned"
        result.size() == 1
        result[0].name == "group"
    }
}