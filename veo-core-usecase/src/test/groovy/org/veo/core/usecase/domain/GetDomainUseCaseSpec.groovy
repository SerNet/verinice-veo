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
package org.veo.core.usecase.domain

import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.repository.DomainRepository
import org.veo.core.usecase.UseCase.IdAndClient
import org.veo.core.usecase.UseCaseSpec

class GetDomainUseCaseSpec extends UseCaseSpec {

    DomainRepository repository = Mock()
    Key existingDomainId

    GetDomainUseCase usecase = new GetDomainUseCase(repository)

    def setup() {
        existingDomainId = Key.newUuid()
        existingDomain.getId() >> existingDomainId
        existingDomain.owner >> existingClient

        repository.getById(existingDomainId) >> existingDomain
        repository.getById(_) >> {throw new NotFoundException("")}
    }

    def "retrieve a domain"() {
        when :
        existingDomain.isActive() >> true
        def output = usecase.execute(new IdAndClient(existingDomainId,  existingClient))
        then:
        output.domain != null
        output.domain.id == existingDomainId
    }

    def "retrieve an inactive domain"() {
        when:
        existingDomain.isActive() >> false
        usecase.execute(new IdAndClient(existingDomainId,  existingClient))
        then:
        thrown(NotFoundException)
    }

    def "retrieve a domain unknown client"() {
        when:
        usecase.execute(new IdAndClient(existingDomainId,  anotherClient))
        then:
        thrown(ClientBoundaryViolationException)
    }

    def "retrieve an unknown domain"() {
        when:
        usecase.execute(new IdAndClient(Key.newUuid(),  existingClient))
        then:
        thrown(NotFoundException)
    }
}
