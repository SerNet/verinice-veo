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


import org.veo.core.entity.Catalog
import org.veo.core.entity.Control
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.LinkTailoringReference
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase.InputData
import org.veo.core.usecase.parameter.IncarnateCatalogItemDescription
import org.veo.core.usecase.parameter.TailoringReferenceParameter

class ApplyIncarnationDescriptionUseCaseSpec extends ApplyIncarnationDescriptionSpec {
    EntityFactory factory = Mock()

    ApplyIncarnationDescriptionUseCase usecase = new ApplyIncarnationDescriptionUseCase(
    unitRepo, catalogItemRepository, entityRepo, designatorService, catalogItemservice, factory)

    def setup() {

        catalogItemservice.createInstance(item1, existingDomain) >> newControl
        entityRepo.getElementRepositoryFor(_) >> repo

        catalogItemRepository.findById(item1.id) >> Optional.of(item1)
        catalogItemRepository.findById(item2.id) >> Optional.of(item2)
        catalogItemRepository.findById(_) >> Optional.empty()
    }

    def "apply an element from item"() {
        given:

        item1.tailoringReferences >> []
        newControl.links >> []

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new IncarnateCatalogItemDescription(item1, [])
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

        Control control2 = Mock()
        Control control3 = Mock()

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.catalog >> catalog
        item2.element>>control2

        LinkTailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.linkType >> "link.type"
        tr.attributes >> null
        tr.referencedElement >> item2

        item1.tailoringReferences >> [tr]
        item1.element >> control

        CustomLink newLink = Mock()
        newLink.target >> control
        newLink.type >> "link.type"

        TailoringReferenceParameter ref = Mock()
        ref.referenceType >> TailoringReferenceType.LINK
        ref.referencedElement >> control3

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1, item2]

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new IncarnateCatalogItemDescription(item1, [ref])
        ]))
        then:
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* factory.createCustomLink(control3, _, "link.type") >> newLink

        output.newElements.size() == 1
        output.newElements.first() == newControl
    }

    def "apply an element from item with external tailor refs"() {
        given:

        CustomLink newLink = Mock()

        factory.createCustomLink(_, _, _) >> newLink

        Control control2 = Mock()
        Control control3 = Mock()

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.catalog >> catalog
        item2.element>>control2

        CustomLink link = Mock()
        link.type >> "external.link.type"
        link.target>>control2
        link.attributes >> [:]

        LinkTailoringReference etr = Mock()
        etr.referenceType >> TailoringReferenceType.LINK_EXTERNAL
        etr.owner >> item1
        etr.catalogItem >> item2
        etr.externalLink >> link

        item1.tailoringReferences >> [etr]
        item1.element >> control

        newControl.links >> []

        TailoringReferenceParameter ref = Mock()
        ref.referencedElement >> control3
        ref.referenceType >> TailoringReferenceType.LINK_EXTERNAL

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1, item2]

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new IncarnateCatalogItemDescription(item1, [ref])
        ]))
        then:
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* control3.addToLinks(_)
        output.newElements.size() == 1
        output.newElements.first() == newControl
    }

    def "apply an element from item with  less tailor refs"() {
        given:

        Control control2 = Mock()

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.catalog >> catalog
        item2.element>>control2

        LinkTailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.catalogItem >> item2

        item1.tailoringReferences >> [tr]
        item1.element >> control

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new IncarnateCatalogItemDescription(item1, [])
        ]))
        then:
        thrown(IllegalArgumentException)
    }

    def "wrong unit"() {
        given:

        item1.tailoringReferences >> []

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]

        def unitId = Key.newUuid()
        Unit anotherUnit = Mock()
        anotherUnit.id >> unitId

        when:
        def output = usecase.execute(new InputData(existingClient, anotherUnit.id, [
            new IncarnateCatalogItemDescription(item1, [])
        ]))
        then:
        thrown(NotFoundException)
    }

    def "wrong item id"() {
        given:

        item1.tailoringReferences >> []

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]

        Catalog other = Mock()
        item2.catalog >> other
        other.domainTemplate >> Mock(Domain)

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [
            new IncarnateCatalogItemDescription(item2, [])
        ]))
        then:
        thrown(ClientBoundaryViolationException)
    }

    def "wrong tailorref"() {
        given:

        item1.tailoringReferences >> []

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]

        TailoringReferenceParameter ref = Mock()
        ref.referenceType >> TailoringReferenceType.LINK_EXTERNAL

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id,
                [
                    new IncarnateCatalogItemDescription(item1, [ref])
                ]))
        then:
        thrown(IllegalArgumentException)
    }
}
