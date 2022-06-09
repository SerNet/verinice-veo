/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.event.ElementTypeDefinitionUpdateEvent
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository

@ContextConfiguration(classes = Config.class)
class ElementTypeDefinitionEntityListenerJpaSpec extends AbstractJpaSpec {
    @Autowired
    ClientDataRepository clientRepo
    @Autowired
    DomainDataRepository domainRepo
    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    ElementTypeDefinitionUpdateEventListener listener

    static class Config {
        @Bean
        public ElementTypeDefinitionUpdateEventListener listener() {
            return new ElementTypeDefinitionUpdateEventListener()
        }
    }

    static class ElementTypeDefinitionUpdateEventListener {
        int eventCount

        @EventListener
        def listen(ElementTypeDefinitionUpdateEvent event) {
            eventCount++
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def 'element type definition update events are published'() {
        when: "creating a domain with an asset definition"
        def domainId = txTemplate.execute {
            def client = clientRepo.save(newClient())
            def domain = domainRepo.save(newDomain(client) {
                elementTypeDefinitions = [
                    newElementTypeDefinition(it, "asset") { assetDefinition ->
                        assetDefinition.subTypes["AST_Server"] = new SubTypeDefinition().tap {
                            it.statuses = ["NEW"]
                        }
                    }
                ]
            })
            clientRepo.save(client)
            domain.idAsString
        }

        then: "no entity type definition update event was published"
        listener.eventCount == 0

        when: "replacing the asset definition"
        txTemplate.execute {
            def domain = domainRepo.findById(domainId).get()
            domain.getElementTypeDefinition(EntityType.ASSET.singularTerm).tap {
                it.subTypes["AST_Server"] = new SubTypeDefinition().tap {
                    it.statuses = ["BRAND_NEW"]
                }
            }
        }

        then: "an entity type definition update event was published"
        listener.eventCount == 1

        when: "replacing the asset definition again"
        txTemplate.execute {
            def domain = domainRepo.findById(domainId).get()
            domain.getElementTypeDefinition(EntityType.ASSET.singularTerm).tap {
                it.subTypes["AST_Server"] = new SubTypeDefinition().tap {
                    it.statuses = ["BRAND_NEW", "USED"]
                }
            }
        }

        then: "another entity type definition update event was published"
        listener.eventCount == 2

        when: "updating the asset definition"
        txTemplate.execute {
            def domain = domainRepo.findById(domainId).get()
            domain.getElementTypeDefinition("asset").subTypes["AST_Server"].statuses.add("OLD")
        }

        then: "an entity type definition update event was published"
        listener.eventCount == 3

        when: "updating the asset definition again"
        txTemplate.execute {
            def domain = domainRepo.findById(domainId).get()
            domain.getElementTypeDefinition("asset").subTypes["AST_Server"].statuses.add("ANCIENT")
        }

        then: "another entity type definition update event was published"
        listener.eventCount == 4
    }
}
