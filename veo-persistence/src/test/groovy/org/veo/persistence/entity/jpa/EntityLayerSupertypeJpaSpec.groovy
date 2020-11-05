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

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

import org.springframework.beans.factory.annotation.Autowired

import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository

class EntityLayerSupertypeJpaSpec extends AbstractJpaSpec {

    public static final String TESTCLIENT_UUID = "274af105-8d21-4f07-8019-5c4573d503e5"

    @Autowired
    AssetDataRepository assetRepository

    @Autowired
    UnitDataRepository unitRepository

    @PersistenceContext
    private EntityManager entityManager

    UnitData owner0
    UnitData owner1
    UnitData owner2

    def setup() {
        owner0 = unitRepository.save(newUnit(null))
        owner1 = unitRepository.save(newUnit(null))
        owner2 = unitRepository.save(newUnit(null))
    }

    def 'finds entities by owners'() {
        given: "three assets from different owners"

        assetRepository.save(newAsset(owner0) {
            name = "asset 0"
        })
        assetRepository.save(newAsset(owner1) {
            name = "asset 1"
        })
        assetRepository.save(newAsset(owner2) {
            name = "asset 2"
        })

        when: "querying assets from the first two owners"
        def result = assetRepository.findEntitiesByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' assets are returned"
        with(result.sort {
            it.name
        }) {
            size() == 2
            it[0].name == "asset 0"
            it[1].name == "asset 1"
        }
    }

    def 'finding entities by owners excludes groups'() {
        given: "a normal asset and an asset group, both from the same owner"
        assetRepository.save(newAsset(owner0) {
            name = "normal"
        })
        assetRepository.save(newAssetGroup(owner0) {
            name = "group"
        })

        when: "querying the owner's assets"
        def result = assetRepository.findEntitiesByUnits([owner0.dbId] as Set)

        then: "only the normal asset is returned"
        result.size() == 1
        result[0].name == "normal"
    }

    def 'finds entity groups by owners'() {
        given: "three asset groups from different owners"
        assetRepository.save(newAssetGroup(owner0) {
            name = "group 0"
        })
        assetRepository.save(newAssetGroup(owner1) {
            name = "group 1"
        })
        assetRepository.save(newAssetGroup(owner2) {
            name = "group 2"
        })

        when: "querying groups from the first two owners"
        def result = assetRepository.findGroupsByUnits([owner0.dbId, owner1.dbId] as Set)

        then: "only the first two owners' assets are returned"
        with(result.sort {
            it.name
        }) {
            size() == 2
            it[0].name == "group 0"
            it[1].name == "group 1"
        }
    }

    def 'finding entity groups by owners excludes normal entities'() {
        given: "a normal asset and an asset group, both from the same owner"
        assetRepository.save(newAsset(owner0) {
            name = "normal"
        })
        assetRepository.save(newAssetGroup(owner0) {
            name = "group"
        })

        when: "querying the owner's asset groups"
        def result = assetRepository.findGroupsByUnits([owner0.dbId] as Set)

        then: "only the group is returned"
        result.size() == 1
        result[0].name == "group"
    }

    def 'increment version id'() {
        given: "one unit db ID"

        def dbId = owner0.getDbId()

        when: "load and save the asset"
        UnitData unit = unitRepository.findById(dbId).get()

        long versionBefore = unit.getVersion()
        unit.setName("New name")
        unitRepository.save(unit)
        entityManager.flush()

        unit = unitRepository.findById(dbId).get()

        then: "version is incremented"
        versionBefore == 0
        unit.getVersion() == 1
    }
}