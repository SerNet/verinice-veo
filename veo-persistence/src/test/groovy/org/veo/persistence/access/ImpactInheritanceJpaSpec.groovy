/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.repository.AssetRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.CustomLinkDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ValidationService

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

class ImpactInheritanceJpaSpec extends AbstractJpaSpec {

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    TransactionTemplate txTemplate

    @Autowired
    CustomLinkDataRepository linkDataRepository

    ValidationService validationMock = Mock()

    AssetRepository assetRepository

    FlyweightLinkDataRepositoryImpl repo

    @Autowired
    @PersistenceContext
    EntityManager em

    Client client
    Unit unit
    Domain domain
    String domainId
    String unitId
    List assets

    def setup() {
        assetRepository = new AssetRepositoryImpl(assetDataRepository, validationMock,
                linkDataRepository, scopeDataRepository, elementQueryFactory)
        repo = new FlyweightLinkDataRepositoryImpl(em)

        txTemplate.execute {
            client = newClient()

            domain = newDomain(client) {
                it.name = "ImpactTestDomain"
                applyElementTypeDefinition(newElementTypeDefinition('asset', it) {
                    subTypes = [
                        SomeAsset: newSubTypeDefinition {
                            statuses = ["NEW"]
                        }
                    ]
                    links = [
                        impactInheritingType1: new LinkDefinition().tap {
                            targetType = "asset"
                            targetSubType = "SomeAsset"
                            attributeDefinitions = [
                                attr: new IntegerAttributeDefinition()
                            ]
                        },
                        impactInheritingType2: new LinkDefinition().tap {
                            targetType = "asset"
                            targetSubType = "SomeAsset"
                            attributeDefinitions = [
                                attr: new IntegerAttributeDefinition()
                            ]
                        }
                    ]
                })
            }
            client = clientRepository.save(client) as Client
            domainId = client.domains.first().idAsString

            def unit = newUnit(client)
            unit = unitRepository.save(unit)
            unitId = unit.idAsString

            assets = []
            (0..5).each { index ->
                def asset = newAsset(unit) {name = "asset$index"}
                assets << assetRepository.save(asset)
            }

            // 0 -> 1 -> 2 -> 3 -> 4
            //      1 -> 5
            //           5      -> 4
            //                3 -> 1
            assets[0].addLink(newCustomLink(assets[1], "impactInheritingType1", domain))
            assets[1].addLink(newCustomLink(assets[2], "impactInheritingType1", domain))
            assets[2].addLink(newCustomLink(assets[3], "impactInheritingType1", domain))
            assets[3].addLink(newCustomLink(assets[4], "impactInheritingType1", domain))
            assets[1].addLink(newCustomLink(assets[5], "impactInheritingType1", domain))
            assets[5].addLink(newCustomLink(assets[4], "impactInheritingType1", domain))
            // a circular relationship:
            assets[3].addLink(newCustomLink(assets[1], "impactInheritingType2", domain))

            assets.each { assetRepository.save(it) }
            client = clientRepository.save(client)
        }
    }

    def "lightweight representations for elements and their links can be loaded"() {
        given:
        def types = [
            "impactInheritingType1",
            "impactInheritingType2"
        ]
        def clientId = client.idAsString
        def result = repo.findAllLinksGroupedByElement(types as Set, domainId, clientId, unitId)

        expect:
        result != null
        result.size() == 5

        def res0 = result.find { it.sourceId() == assets[0].idAsString }
        res0 != null
        res0.links.size() == 1
        res0.links.first().type == "impactInheritingType1"
        res0.links.first().targetId() == assets[1].idAsString

        def res1 = result.find { it.sourceId() == assets[1].idAsString }
        res1 != null
        res1.links.size() == 2
        res1.links.every { link -> link.type == "impactInheritingType1" }
        res1.links.collect {it.targetId() } ==~ [
            assets[2].idAsString,
            assets[5].idAsString
        ]

        def res2 = result.find { it.sourceId() == assets[2].idAsString }
        res2 != null
        res2.links.size() == 1
        res2.links.first().type == "impactInheritingType1"
        res2.links.first().targetId() == assets[3].idAsString

        def res3 = result.find { it.sourceId() == assets[3].idAsString }
        res3 != null
        res3.links.size() == 2
        res3.links.collect{it.type()} ==~ [
            "impactInheritingType1",
            "impactInheritingType2"
        ]
        res3.links.collect{it.targetId()} ==~ [
            assets[4].idAsString,
            assets[1].idAsString
        ]

        def res5 = result.find { it.sourceId() == assets[5].idAsString }
        res5 != null
        res5.links.size() == 1
        res5.links.first().type == "impactInheritingType1"
        res5.links.first().targetId() == assets[4].idAsString
    }
}
