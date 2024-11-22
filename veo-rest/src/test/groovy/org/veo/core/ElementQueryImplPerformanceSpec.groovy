/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.GenericElementRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ProcessData

import net.ttddyy.dsproxy.QueryCountHolder

class ElementQueryImplPerformanceSpec extends AbstractPerformanceITSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private DomainRepository domainRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private AssetRepositoryImpl assetRepository

    @Autowired
    private ProcessRepositoryImpl processRepository

    @Autowired
    private GenericElementRepositoryImpl elementRepository

    private Client client
    Domain domain
    private Unit unit

    def setup() {
        client = clientRepository.save(newClient {
            newDomain(it) {
                getElementTypeDefinition("process").with{
                    subTypes = [
                        "NormalProcess": newSubTypeDefinition {
                            statuses = ["NEW"]
                        }
                    ]
                    customAspects = [
                        my_custom_aspect: newCustomAspectDefinition {
                            attributeDefinitions = [
                                foo: new TextAttributeDefinition()
                            ]
                        }
                    ]
                }
            }
        })
        domain = client.domains.first()
        unit = unitRepository.save(newUnit(client))
    }

    def "query efficiently fetches results"() {
        given:
        QueryCountHolder.clear()
        final def testProcessCount = 10

        def asset = assetRepository.save(newAsset(unit))
        def processes = new HashSet<ProcessData>()
        for(int i = 0; i < testProcessCount; i++) {
            processes.add(newProcess(unit) {
                associateWithDomain(domain, "VT", "NEW")
                applyCustomAspect(newCustomAspect("my_custom_aspect", domain) {
                    attributes = [
                        "foo": "bar"
                    ]
                })
                links = [
                    newCustomLink(asset, "my_little_link", domain)
                ]
            })
        }
        processRepository.saveAll(processes)
        QueryCountHolder.grandTotal.select == 0

        when:
        def result = txTemplate.execute {
            processRepository.query(client).execute(PagingConfiguration.UNPAGED)
        }

        then: "all data has been fetched"
        result.totalResults == testProcessCount
        with(result.resultPage[0]) {
            customAspects.first().attributes["foo"] == "bar"
            domains.first() != null
            getSubType(domain) != null
            links.first() != null
        }

        QueryCountHolder.grandTotal.select == 6
        QueryCountHolder.grandTotal.time < 500
    }
}
