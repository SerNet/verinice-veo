/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.TailoringReferenceType
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.ProfileDataRepository

class ProfileJpaSpec extends AbstractJpaSpec{
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    DomainDataRepository domainRepository
    @Autowired
    ProfileDataRepository profileRepository
    @Autowired
    TransactionTemplate txTemplate

    Client client
    Domain domain

    def setup() {
        client = clientRepository.save(newClient() {
            domains = [newDomain(it)]
        })
        domain = client.domains.first()
    }

    def "can persist profile #type tailoring references"() {
        given:
        def profileId = txTemplate.execute {
            profileRepository.save(newProfile(domain) {
                def item1 = newProfileItem(it) {
                    name = "target"
                    elementType = "asset"
                    subType = "assetoBalsamico"
                    status = "delicious"
                }
                def item2 = newProfileItem(it) {
                    name = "source"
                    elementType = "asset"
                    subType = "assetoBalsamico"
                    status = "delicious"
                    tailoringReferences = [
                        newTailoringReference(it, item1, type)
                    ]
                }
                items = [
                    item1,
                    item2
                ]
            }).dbId
        }

        when:
        def profile = profileRepository.findById(client.dbId, profileId).get()

        then:
        profile.items.size() == 2
        with(profile.items.find { it.name == "source" }.tailoringReferences[0]) {
            target.name == "target"
            referenceType == type
        }

        where:
        type << TailoringReferenceType.values()
    }
}
