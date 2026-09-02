/*
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler.
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
 */
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.IncarnationConfiguration
import org.veo.core.entity.IncarnationLookup
import org.veo.core.entity.IncarnationRequestModeType
import org.veo.core.entity.Profile
import org.veo.core.entity.ProfileItem
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.rest.security.NoRestrictionAccessRight

@WithUserDetails("user@domain.example")
class GetProfileIncarnationUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase

    @Autowired
    private UnitRepository unitRepository

    def "getProfileIncarnationDescriptionUseCase not compact"() {
        given: 'a unit with example elements'
        def client = createTestClient()
        def domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.getById(client.id)
        def unit = unitRepository.save(newUnit(client))
        def profile = domain.profiles.first()

        when:
        def result = getIncarnationDescriptions(unit, domain, null, profile, false).references

        then: 'all tailoring references are returned'
        result.collectMany{it.references}.size() == 23

        when: "we get only the distinct tailoring references"
        result = getIncarnationDescriptions(unit, domain, null, profile, true).references

        then: 'less tailoring references are returned'
        result.collectMany{it.references}.size() == 12
    }

    def "references types can be excluded"() {
        given: 'a domain with linked profile items, but with links excluded from incarnation'
        def client = createTestClient()
        def domain = domainDataRepository.save(newDomain(client) { Domain d ->
            incarnationConfiguration = new IncarnationConfiguration(IncarnationRequestModeType.DEFAULT, IncarnationLookup.ALWAYS, null, [TailoringReferenceType.LINK] as Set)
            profiles = [
                newProfile(d) { Profile p ->
                    def target = newProfileItem(p) {
                        name = "target"
                        elementType = ElementType.CONTROL
                        subType = "c"
                        status = "BLUE"
                    }
                    def source = newProfileItem(p) {
                        name = "source"
                        elementType = ElementType.SCENARIO
                        subType = "s"
                        status = "RED"
                        addLinkTailoringReference(TailoringReferenceType.LINK, target, "someLink", [:])
                    }
                    items = [source, target]
                }
            ]
        })
        client = clientRepository.getById(client.id)
        def unit = unitRepository.save(newUnit(client))
        def profile = domain.profiles.first()
        def linkSourceProfileItem = profile.items.find { it.name == "source" }

        when: 'planning to incarnate only the link source'
        def result = getIncarnationDescriptions(unit, domain, [linkSourceProfileItem], profile, false).references

        then: 'the link is excluded'
        result*.item*.name == ["source"]
        result.collectMany { it.references }.size() == 0

        when: 'planning to incarnate the entire profile'
        result = executeInTransaction {
            getIncarnationDescriptions(unit, domain, null, profile, false).references
        }

        then: 'the link is excluded'
        result*.item*.name ==~ ["source", "target"]
        result.collectMany { it.references }.size() == 0
    }

    private GetProfileIncarnationDescriptionUseCase.OutputData getIncarnationDescriptions(
            Unit unit,
            Domain domain,
            List<ProfileItem> profileItems,
            Profile profile,
            Boolean mergeBidirectionalReferences) {
        executeInTransaction {
            getProfileIncarnationDescriptionUseCase.execute(
                    new GetProfileIncarnationDescriptionUseCase.InputData(
                    unit.id,
                    domain.id,
                    profileItems?.collect { it.symbolicId },
                    profile.id,
                    mergeBidirectionalReferences),
                    NoRestrictionAccessRight.from(domain.owner.idAsString))
        }
    }
}