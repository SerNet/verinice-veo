/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.exception.ReferenceTargetNotFoundException
import org.veo.core.repository.DomainRepository
import org.veo.core.usecase.common.NameableInputData
import org.veo.core.usecase.unit.CreateUnitUseCase
import org.veo.core.usecase.unit.CreateUnitUseCase.InputData

public class CreateUnitUseCaseSpec extends UseCaseSpec {
    DomainRepository domainRepository = Mock()
    CreateUnitUseCase usecase = new CreateUnitUseCase(clientRepository, unitRepository, domainRepository, entityFactory)

    def "Create new unit in a client" () {
        Unit newUnit1 = Mock()
        newUnit1.id >> UUID.randomUUID()
        newUnit1.name >> "New unit"

        given: "starting values for a unit"
        def namedInput = new NameableInputData("New unit", null, null)

        when: "the use case to create a unit is executed"
        def input = new InputData(namedInput, 1, [] as Set)
        def newUnit = usecase.execute(input, noRestrictionExistingClient).unit

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "a new unit was created and stored"
        1 * entityFactory.createUnit("New unit") >> newUnit1
        1 * unitRepository.save(newUnit1) >> newUnit1
        newUnit != null
        newUnit.getName() == "New unit"
    }

    def "Create a unit with a domain" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData("New unit", null, null)

        Unit newUnit1 = Mock()
        newUnit1.id >> UUID.randomUUID()
        newUnit1.name >> "New unit"

        entityFactory.createUnit(_) >> newUnit1

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, 1, [existingDomain.id] as Set)

        when: "the use case to create a unit is executed"
        usecase.execute(input,noRestrictionExistingClient)

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "the unit is created with the expected domains"
        1 * domainRepository.getByIds([existingDomain.id] as Set, existingClient.id) >> [existingDomain]
        1 * newUnit1.addToDomains([existingDomain] as Set)
        0 * newUnit1.addToDomains(_)
        1 * unitRepository.save(_) >> newUnit1
    }

    def "Create a unit with an invalid domain" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData("New unit", null, null)

        Unit newUnit1 = Mock()
        newUnit1.id >> UUID.randomUUID()
        newUnit1.name >> "New unit"

        entityFactory.createUnit(_) >> newUnit1

        def randomDomainId = UUID.randomUUID()

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, 1, [randomDomainId] as Set)

        when: "the use case to create a unit is executed"
        usecase.execute(input,noRestrictionExistingClient)

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "the unit is created with the expected domains"
        1 * domainRepository.getByIds([randomDomainId] as Set, existingClient.id) >> { throw new NotFoundException(randomDomainId, Domain) }
        thrown(ReferenceTargetNotFoundException)
    }

    def "Create a unit with another client's domain" () {
        given: "starting values for a unit"
        def namedInput = new NameableInputData("New unit", null, null)

        Unit newUnit1 = Mock()
        newUnit1.id >> UUID.randomUUID()
        newUnit1.name >> "New unit"

        Domain anotherDomain = Mock()
        anotherDomain.id >> UUID.randomUUID()
        anotherClient.getDomains() >> [anotherDomain]
        anotherDomain.getOwner() >> anotherClient
        entityFactory.createUnit(_) >> newUnit1

        and: "a parent unit in an existing client"
        def input = new InputData(namedInput, 1, [anotherDomain.id] as Set)

        when: "the use case to create a unit is executed"
        usecase.execute(input,noRestrictionExistingClient)

        then: "a client was retrieved"
        1 * clientRepository.findById(_) >> Optional.of(this.existingClient)

        and: "the unit is created with the expected domains"
        1 * domainRepository.getByIds([anotherDomain.id] as Set, existingClient.id) >> { throw new NotFoundException(anotherDomain.id, Domain) }
        thrown(ReferenceTargetNotFoundException)
    }
}
