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
import org.veo.core.entity.Key
import org.veo.core.entity.LinkTailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase.InputData

class GetAndApplyIncarnationDescriptionUseCaseSpec extends ApplyIncarnationDescriptionSpec {
    EntityFactory factory = Mock()
    GetIncarnationDescriptionUseCase usecaseGet = new GetIncarnationDescriptionUseCase(unitRepo, catalogItemRepository, entityRepo)

    ApplyIncarnationDescriptionUseCase usecasePut = new ApplyIncarnationDescriptionUseCase(
    unitRepo, catalogItemRepository, domainRepository, entityRepo, designatorService, factory)

    def setup() {
        item1.incarnate() >> newControl
        entityRepo.getElementRepositoryFor(_) >> repo
        repo.query(existingClient) >> emptyQuery

        catalogItemRepository.getByIdsFetchElementData([item1.id] as Set) >> [item1]

        domainRepository
                .findByCatalogItem(item1) >> Optional.of(existingDomain)
    }

    def "get the apply information for a catalog-item without tailor reference and apply it"() {
        given:
        def id = Key.newUuid()
        item1.id >> id
        item1.catalog >> catalog
        item1.tailoringReferences >> []
        newControl.links >> []

        catalogItemRepository.getById(id) >> item1

        when: "get the apply data for item"
        def output = usecaseGet.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then: "the parameter object is return"
        output.references.size() == 1
        output.references.first().item == item1
        output.references.first().references == []

        when: "use this parameter object"
        def o1 = usecasePut.execute(new ApplyIncarnationDescriptionUseCase.InputData(existingClient, existingUnit.id, output.references))

        then: "the new element is created and saved"
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)

        o1.newElements.size() == 1
        o1.newElements.first() == newControl
    }

    def "get the apply information for a catalog-item"() {
        given:
        def id = Key.newUuid()
        item1.id >> id
        item1.catalog >> catalog
        item1.elementInterface >> Control.class

        Control control2 = Mock()
        control2.getModelInterface() >> Control.class

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.catalog >> catalog
        item2.elementInterface >> Control.class

        LinkTailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.linkType >> "link.type"
        tr.catalogItem >> item2

        item1.tailoringReferences >> [tr]

        CustomLink newLink = Mock()

        when: "request to create an item with a link"
        def output = usecaseGet.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then: "we got the parameter objekt"
        output.references.size()== 1
        output.references.first().item == item1
        output.references.first().references.size() == 1
        output.references.first().references.first().referenceKey == "link.type"

        when: "we set control2 as the target of the link"
        output.references.first().references.first().referencedElement = control2
        def o1 = usecasePut.execute(new ApplyIncarnationDescriptionUseCase.InputData(existingClient, existingUnit.id, output.references))

        then: "the control is saved and the link ist set to control2"
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* factory.createCustomLink(control2, newControl, "link.type", _) >> newLink

        o1.newElements.size() == 1
        o1.newElements.first() == newControl
    }
}
