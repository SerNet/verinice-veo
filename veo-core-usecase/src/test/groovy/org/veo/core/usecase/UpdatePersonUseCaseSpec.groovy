/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase

import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.state.CompositeElementState
import org.veo.core.repository.PersonRepository
import org.veo.core.usecase.base.ModifyElementUseCase.InputData
import org.veo.core.usecase.common.ETag
import org.veo.core.usecase.decision.Decider
import org.veo.core.usecase.person.UpdatePersonUseCase
import org.veo.core.usecase.service.EntityStateMapper

public class UpdatePersonUseCaseSpec extends UseCaseSpec {

    PersonRepository personRepository = Mock()
    Decider decider = Mock()
    EntityStateMapper entityStateMapper = new EntityStateMapper()
    UpdatePersonUseCase usecase = new UpdatePersonUseCase(repositoryProvider, decider, entityStateMapper, refResolverFactory)

    def "update a person"() {
        given:
        def id = Key.newUuid()
        Person person = Mock()
        person.id >> id
        person.idAsString >> id.uuidValue()
        person.getOwner() >> existingUnit
        person.name >> "Updated person"
        person.version >> 0
        person.domains >> []
        person.domainTemplates >> []
        person.customAspects >> []
        person.links >> []
        person.parts >> []
        person.composites >> []
        person.scopes >> []
        person.appliedCatalogItems >> []

        CompositeElementState personState = Mock {
            getId() >> id.uuidValue()
            domainAssociationStates >> []
            parts >> []
        }

        when:
        def eTag = ETag.from(person.getId().uuidValue(), 0)
        def output = usecase.execute(new InputData(id.uuidValue(), personState, existingClient, eTag, "max"))

        then:
        1 * repositoryProvider.getElementRepositoryFor(Person) >> personRepository
        1 * personRepository.save(person) >> person
        1 * personRepository.findById(person.id) >> Optional.of(person)
        1 * personRepository.getById(person.id, existingClient.id) >> person
        output.entity != null
        output.entity.name == "Updated person"
    }
}
