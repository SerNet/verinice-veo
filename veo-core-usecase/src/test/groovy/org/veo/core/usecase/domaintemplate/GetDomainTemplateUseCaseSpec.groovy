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
package org.veo.core.usecase.domaintemplate

import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.Key
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.ClientRepository
import org.veo.core.service.DomainTemplateService
import org.veo.core.usecase.UseCase.IdAndClient
import org.veo.core.usecase.UseCaseSpec

class GetDomainTemplateUseCaseSpec extends UseCaseSpec {

    DomainTemplateService templateService = Mock()
    ClientRepository clientRepository = Mock()

    GetDomainTemplateUseCase usecase = new GetDomainTemplateUseCase(templateService, clientRepository)

    def "retrieve a domaintemplate"() {
        given:
        def id = Key.newUuid()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id
        templateService.getTemplate(existingClient, id) >> Optional.of(domaintemplate)
        clientRepository.findById(existingClient.id) >> Optional.of(existingClient)

        when:
        def output = usecase.execute(new IdAndClient(id,  existingClient))
        then:
        output.domainTemplate != null
        output.domainTemplate.id == id
    }

    def "retrieve a domaintemplate unknown client"() {
        given:
        def id = Key.newUuid()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id

        clientRepository.findById(existingClient.id) >> Optional.empty()
        when:
        usecase.execute(new IdAndClient(id,  existingClient))
        then:
        thrown(NotFoundException)
    }
}
