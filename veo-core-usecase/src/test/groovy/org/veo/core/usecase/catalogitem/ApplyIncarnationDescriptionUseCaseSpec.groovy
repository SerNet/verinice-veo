/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.usecase.catalogitem

import org.veo.core.entity.Control
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.LinkTailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase.InputData
import org.veo.core.usecase.parameter.TailoringReferenceParameter
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription

class ApplyIncarnationDescriptionUseCaseSpec extends ApplyIncarnationDescriptionSpec {
    EntityFactory factory = Mock()

    ApplyIncarnationDescriptionUseCase usecase = new ApplyIncarnationDescriptionUseCase(
    unitRepo, catalogItemRepository, domainRepository,
    entityRepo, designatorService, factory)

    def setup() {

        item1.incarnate() >> newControl
        entityRepo.getElementRepositoryFor(_) >> repo

        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences([item1.id] as Set) >> [item1]
        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences([item2.id] as Set) >> [item2]

        domainRepository
                .findByCatalogItem(item1) >> Optional.of(existingDomain)
    }

    def "apply an element from item"() {
        given:
        item1.tailoringReferences >> []
        newControl.links >> []

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new TemplateItemIncarnationDescription(item1, [])
        ]))

        then:
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)

        output.newElements.size() == 1
        output.newElements.first() == newControl
    }

    def "apply an element from item with tailor refs"() {
        given:
        Control control3 = Mock()
        def trId = Key.newUuid()

        item2.id >> Key.newUuid()
        item2.owner >> existingDomain

        LinkTailoringReference tr = Mock()
        tr.id >> trId
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.linkType >> "link.type"
        tr.attributes >> null
        tr.referencedElement >> item2

        item1.tailoringReferences >> [tr]

        CustomLink newLink = Mock()
        newLink.target >> control
        newLink.type >> "link.type"

        TailoringReferenceParameter ref = Mock()
        ref.id >> trId.uuidValue()
        ref.referenceType >> TailoringReferenceType.LINK
        ref.referencedElement >> control3

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new TemplateItemIncarnationDescription(item1, [ref])
        ]))

        then:
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* factory.createCustomLink(control3, _, "link.type", _) >> newLink

        output.newElements.size() == 1
        output.newElements.first() == newControl
    }

    def "apply an element from item with external tailor refs"() {
        given:
        CustomLink newLink = Mock()
        def trId = Key.newUuid()

        factory.createCustomLink(_, _, _, _) >> newLink

        Control control2 = Mock()
        Control control3 = Mock()
        control3.domains >> [existingDomain]

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.owner >> existingDomain

        CustomLink link = Mock()
        link.type >> "external.link.type"
        link.target>>control2
        link.attributes >> [:]

        LinkTailoringReference etr = Mock()
        etr.id >> trId
        etr.referenceType >> TailoringReferenceType.LINK_EXTERNAL
        etr.owner >> item1
        etr.target >> item2
        etr.externalLink >> link

        item1.tailoringReferences >> [etr]

        newControl.links >> []

        TailoringReferenceParameter ref = Mock()
        ref.id >> trId.uuidValue()
        ref.referencedElement >> control3
        ref.referenceType >> TailoringReferenceType.LINK_EXTERNAL

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new TemplateItemIncarnationDescription(item1, [ref])
        ]))

        then:
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* control3.applyLink(_)
        output.newElements.size() == 1
        output.newElements.first() == newControl
    }

    def "apply an element from item with  less tailor refs"() {
        given:
        def id2 = Key.newUuid()
        item2.id >> id2
        item2.owner >> existingDomain

        LinkTailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.target >> item2

        item1.tailoringReferences >> [tr]

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id, [
            new TemplateItemIncarnationDescription(item1, [])
        ]))

        then:
        thrown(IllegalArgumentException)
    }

    def "wrong unit"() {
        given:
        item1.tailoringReferences >> []

        def unitId = Key.newUuid()
        Unit anotherUnit = Mock()
        anotherUnit.id >> unitId

        when:
        usecase.execute(new InputData(existingClient, anotherUnit.id, [
            new TemplateItemIncarnationDescription(item1, [])
        ]))

        then:
        thrown(NotFoundException)
    }

    def "wrong item id"() {
        given:
        item1.tailoringReferences >> []

        def otherDomainId = Key.newUuid()
        Domain otherDomain = Mock(Domain)
        otherDomain.id >> otherDomainId
        otherDomain.modelInterface >> Domain.class
        domainRepository
                .findByCatalogItem(item2) >> Optional.of(otherDomain)
        domainRepository.getById(otherDomainId) >> otherDomain

        item2.owner >> otherDomain
        item2.requireDomainMembership() >> otherDomain

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id, [
            new TemplateItemIncarnationDescription(item2, [])
        ]))

        then:
        thrown(ClientBoundaryViolationException)
    }

    def "wrong tailorref"() {
        given:
        item1.tailoringReferences >> []

        TailoringReferenceParameter ref = Mock()
        ref.referenceType >> TailoringReferenceType.LINK_EXTERNAL

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id,
                [
                    new TemplateItemIncarnationDescription(item1, [ref])
                ]))

        then:
        thrown(IllegalArgumentException)
    }
}
