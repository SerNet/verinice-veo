/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
import org.veo.core.entity.Domain
import org.veo.core.entity.Profile
import org.veo.core.entity.ProfileItem
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ProfileItemRepository
import org.veo.core.usecase.catalogitem.IncarnationDescriptionApplier
import org.veo.core.usecase.parameter.TailoringReferenceParameter
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription

@WithUserDetails("user@domain.example")
class IncarnationDescriptionApplierITSpec extends VeoSpringSpec {
    @Autowired IncarnationDescriptionApplier incarnationDescriptionApplier
    @Autowired ProfileItemRepository profileItemRepository
    @Autowired AssetRepository assetRepository
    private Client client
    private Unit unit
    private Domain domain
    private ProfileItem subControl
    private ProfileItem superControl
    private ProfileItem asset

    def setup() {
        client = clientRepository.save(newClient {})
        domain = domainDataRepository.save(newDomain(client) {
            elementTypeDefinitions.each {
                it.subTypes['A'] = newSubTypeDefinition {}
            }
            profiles.add(newProfile(it) {
                name = "Provielfältig"
                def subControl = newProfileItem(it) {
                    name = "sub control"
                    elementType = "control"
                    subType = "A"
                    status = "NEW"
                }
                def superControl = newProfileItem(it) {
                    name = "super control"
                    elementType = "control"
                    subType = "A"
                    status = "NEW"
                }
                def asset = newProfileItem(it) {
                    name = "asset"
                    elementType = "asset"
                    subType = "A"
                    status = "NEW"
                }
                subControl.addTailoringReference(TailoringReferenceType.COMPOSITE, superControl)
                superControl.addTailoringReference(TailoringReferenceType.PART, subControl)
                asset.addControlImplementationReference(superControl, null, null)
                items = [
                    subControl,
                    superControl,
                    asset
                ]
            })
        })

        // Retrieve proper profile item JPA entities
        domain.profiles.first().items.with {
            subControl = it.find { it.name == "sub control" }
            superControl = it.find { it.name == "super control" }
            asset = it.find { it.name == "asset" }
        }
        unit = unitDataRepository.save(newUnit(client) {
            domains = [domain]
        })
    }

    def "CI tailoring reference is applied when target is passed first"() {
        when:
        executeInTransaction {
            incarnationDescriptionApplier.incarnate(unit.id, [
                createIncarnationDescription(asset),
                createIncarnationDescription(superControl),
                createIncarnationDescription(subControl),
            ], profileItemRepository, client)
        }
        def asset = executeInTransaction {
            assetRepository.findByDomain(domain).first().tap {
                // wake up lazy things
                it.controlImplementations*.control
                it.requirementImplementations*.control
            }
        }

        then:
        asset.name == "asset"
        asset.controlImplementations.size() == 1
        asset.requirementImplementations.size() == 2
    }

    TemplateItemIncarnationDescriptionState<ProfileItem, Profile> createIncarnationDescription(ProfileItem item) {
        return new TemplateItemIncarnationDescription<>(item, item.tailoringReferences.collect {
            new TailoringReferenceParameter(null, it.referenceType, null, it.idAsString)
        })
    }
}
