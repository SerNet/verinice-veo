/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import java.time.LocalDate

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Client
import org.veo.core.entity.ElementType
import org.veo.core.entity.RequirementImplementationTailoringReference
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ProfileRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.domain.CreateProfileFromUnitUseCase
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.rest.security.NoRestrictionAccessRight

@WithUserDetails("content-creator")
class CreateProfileFromUnitUseCaseITSpec extends VeoSpringSpec{
    @Autowired
    DomainDataRepository domainRepository

    @Autowired
    DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    UnitRepository unitRepository

    @Autowired
    AssetRepository assetRepository

    @Autowired
    ProfileRepository profileRepository

    @Autowired
    CreateProfileFromUnitUseCase useCase

    Client client

    def setup() {
        client = createTestClient()
    }

    def "create profile from unit"() {
        given:
        def domain = domainRepository.save(newDomain(client))
        def unit = unitRepository.save(newUnit(client))
        def person = personDataRepository.save(newPerson(unit) {
            name = 'Peter'
            associateWithDomain(domain, "Person", "overpaid")
        })
        def control = controlDataRepository.save(newControl(unit) {
            associateWithDomain(domain, "Control", "nasty")
        })
        def document = documentDataRepository.save(newDocument(unit) {
            name = "Protocol"
            associateWithDomain(domain, "Document", "boring")
        })
        def asset = assetRepository.save(newAsset(unit) {
            associateWithDomain(domain, "Asset", "good")
            implementControl(control)
            it.requirementImplementations.first().tap {
                cost = 5000
                implementedBy = person
                implementationDate = LocalDate.of(1999,4, 1)
                implementationUntil = LocalDate.of(2000, 1, 1)
                it.document = document
                lastRevisionBy = person
                lastRevisionDate = LocalDate.of(2010, 1, 1)
                nextRevisionBy = person
                nextRevisionDate = LocalDate.of(2100, 1, 1)
            }
        })

        when:
        def profileItems = executeInTransaction {
            useCase.execute(new CreateProfileFromUnitUseCase.InputData(domain.id, unit.id, null, "Profile 1", "A very good profile", "klingon", "p1"), NoRestrictionAccessRight.from(client.idAsString))
        }.profile().items

        then:
        profileItems.size() == 4

        when:
        def assetPI = profileItems.find {it.elementType == ElementType.ASSET}
        def trs = assetPI.tailoringReferences

        then:
        trs.size() == 2
        with(trs.findAll {it instanceof RequirementImplementationTailoringReference}.first() as RequirementImplementationTailoringReference) {
            cost == 5000
            implementedBy.name == 'Peter'
            implementationDate.toString() == '1999-04-01'
            implementationUntil.toString() == '2000-01-01'
            document.name == 'Protocol'
            lastRevisionBy.name == 'Peter'
            lastRevisionDate.toString() == '2010-01-01'
            nextRevisionBy.name == 'Peter'
            nextRevisionDate.toString() == '2100-01-01'
        }
    }
}
