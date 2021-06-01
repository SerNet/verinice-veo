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
import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.CustomLink
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.repository.ElementQuery
import org.veo.core.repository.ElementRepository
import org.veo.core.repository.PagedResult
import org.veo.core.repository.RepositoryProvider
import org.veo.core.repository.UnitRepository
import org.veo.core.service.CatalogItemService
import org.veo.core.service.DomainTemplateService
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.UseCaseSpec
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase.InputData
import org.veo.core.usecase.parameter.TailoringReferenceParameter

class GetAndApplyIncarnationDescriptionUseCaseSpec extends ApplyIncarnationDescriptionSpec {
    EntityFactory factory = Mock()
    GetIncarnationDescriptionUseCase usecaseGet = new GetIncarnationDescriptionUseCase(unitRepo, catalogItemRepository, entityRepo)

    ApplyIncarnationDescriptionUseCase usecasePut = new ApplyIncarnationDescriptionUseCase(
    unitRepo, catalogItemRepository,entityRepo, designatorService, catalogItemservice, factory)


    def setup() {
        catalogItemservice.createInstance(item1, existingDomain) >> newControl
        entityRepo.getElementRepositoryFor(_) >> repo
        repo.query(existingClient) >> emptyQuery

        catalogItemRepository.findById(item1.id) >> Optional.of(item1)
        catalogItemRepository.findById(item2.id) >> Optional.of(item2)
        catalogItemRepository.findById(_) >> Optional.empty()
    }

    def "get the apply information for a catalog-item without tailor reference and apply it"() {
        given:

        def id = Key.newUuid()
        item1.id >> id
        item1.catalog >> catalog
        item1.tailoringReferences >> []
        item1.element >> control
        control.getModelInterface() >> Control.class
        newControl.links >> []

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1]
        catalogItemRepository.findById(id) >> Optional.of(item1)


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

        Control control2 = Mock()
        control.getModelInterface() >> Control.class

        def id2 = Key.newUuid()
        item2.id >> id2
        item2.catalog >> catalog
        item2.element>>control2

        TailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.catalogItem >> item2

        item1.tailoringReferences >> [tr]
        item1.element >> control

        CustomLink link = Mock()
        link.type >> "link.type"
        link.target >>control2

        control.links >> [link]

        CustomLink newLink = Mock()
        newLink.target >> control2
        newControl.links >> [newLink]

        existingDomain.catalogs >> [catalog]
        catalog.domainTemplate >> existingDomain
        catalog.catalogItems >> [item1, item2]

        when: "request to create an item with a link"
        def output = usecaseGet.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then: "we got the parameter objekt"
        output.references.size()== 1
        output.references.first().item == item1
        output.references.first().references.size() == 1
        output.references.first().references.first().referenceKey == "link.type"

        when: "we set control2 as the target of the link"
        output.references.first().references.first().referencedCatalogable = control2
        def o1 = usecasePut.execute(new ApplyIncarnationDescriptionUseCase.InputData(existingClient, existingUnit.id, output.references))

        then: "the control is saved and the link ist set to control2"
        1* repo.save(newControl) >> newControl
        1* newControl.setOwner(existingUnit)
        1* designatorService.assignDesignator(newControl, existingClient)
        1* newLink.setTarget(control2)

        o1.newElements.size() == 1
        o1.newElements.first() == newControl
    }
}
