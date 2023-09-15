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
import org.veo.core.entity.Key
import org.veo.core.entity.LinkTailoringReference
import org.veo.core.entity.TailoringReference
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.RuntimeModelException
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase.InputData

class GetIncarnationDescriptionUseCaseSpec extends ApplyIncarnationDescriptionSpec {

    GetIncarnationDescriptionUseCase usecase = new GetIncarnationDescriptionUseCase(unitRepo, catalogItemRepository, entityRepo)

    def setup() {
        entityRepo.getElementRepositoryFor(_) >> repo
        repo.query(existingClient) >> emptyQuery

        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences([item1.id] as Set) >> [item1]
        catalogItemRepository.findAllByIdsFetchDomainAndTailoringReferences(_) >> []

        domainRepository
                .findByCatalogItem(item1) >> Optional.of(existingDomain)
    }

    def "get the apply information for a catalog-item without tailorref"() {
        given:
        item1.tailoringReferences >> []

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then:
        output.references.size() == 1
        output.references.first().item == item1
        output.references.first().references == []
    }

    def "get the apply information for a catalog-item with one copy ref"() {
        given:
        def id2 = Key.newUuid()
        item2.id >> id2
        item2.owner >> existingDomain
        item2.elementType >> "control"
        item2.tailoringReferences >> []

        def trId = Key.newUuid()
        TailoringReference tr = Mock()
        tr.id >> trId
        tr.referenceType >> TailoringReferenceType.COPY
        tr.owner >> item1
        tr.target >> item2

        item1.tailoringReferences >> [tr]

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then:
        output.references.size() == 2
        output.references.first().item == item1
        output.references.first().references == []
    }

    def "get the apply information for a catalog-item with link"() {
        given:
        def id2 = Key.newUuid()
        item2.id >> id2
        item2.owner >> existingDomain
        item2.elementType >> "control"
        item2.elementInterface >> Control.class

        def trId = Key.newUuid()
        LinkTailoringReference tr = Mock()
        tr.id >> trId
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.linkType >> "link.type"
        tr.target >> item2

        item1.tailoringReferences >> [tr]
        item1.elementType >> "control"
        item1.elementInterface >> Control.class

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then:
        output.references.size()== 1
        with(output.references.first()) {
            item == item1
            references.size() == 1
            references.first().referenceKey == "link.type"
            references.first().referenceType == TailoringReferenceType.LINK
        }
    }

    def "get the apply information for a catalog-item with external link"() {
        given:
        def id2 = Key.newUuid()
        item2.id >> id2
        item2.owner >> existingDomain
        item2.elementType >> "control"
        item2.elementInterface >> Control.class

        def trId = Key.newUuid()
        LinkTailoringReference tr = Mock()
        tr.id >> trId
        tr.referenceType >> TailoringReferenceType.LINK_EXTERNAL
        tr.linkType >> "external.link.type"
        tr.owner >> item1
        tr.target >> item2

        item1.tailoringReferences >> [tr]
        item1.elementType >> "control"
        item1.elementInterface >> Control.class

        when:
        def output = usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then:
        output.references.size()== 1
        with(output.references.first()) {
            item == item1
            references.size() == 1
            references[0].referenceKey == "external.link.type"
            references[0].referenceType == TailoringReferenceType.LINK_EXTERNAL
        }
    }

    def "get the apply information for a catalog-item with link to an unknown feature "() {
        given:
        item2.elementType >> "no valid element type"
        item2.getElementType() >> "not known"

        LinkTailoringReference tr = Mock()
        tr.referenceType >> TailoringReferenceType.LINK
        tr.owner >> item1
        tr.target >> item1
        item1.tailoringReferences >> [tr]
        item1.getAppliedCatalogItems() >> []
        item1.getElementType() >> "control"

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id]))

        then:
        thrown(RuntimeModelException)
    }

    def "wrong unit"() {
        given:
        item1.tailoringReferences >> []

        def unitId = Key.newUuid()
        Unit anotherUnit = Mock()
        anotherUnit.id >> unitId

        when:
        usecase.execute(new InputData(existingClient, anotherUnit.id, [item1.id]))

        then:
        thrown(NotFoundException)
    }

    def "wrong item id"() {
        given:
        item1.tailoringReferences >> []

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id, [Key.newUuid()]))

        then:
        thrown(NotFoundException)
    }

    def "not unique items"() {
        given:
        item1.tailoringReferences >> []

        when:
        usecase.execute(new InputData(existingClient, existingUnit.id, [item1.id, item1.id]))

        then:
        thrown(IllegalArgumentException)
    }
}
