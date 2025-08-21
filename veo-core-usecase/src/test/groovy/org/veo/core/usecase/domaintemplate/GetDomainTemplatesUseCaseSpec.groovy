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

import org.veo.core.entity.Client
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.exception.NotFoundException
import org.veo.core.repository.ClientRepository
import org.veo.core.service.DomainTemplateService
import org.veo.core.usecase.UseCase.EmptyInput
import org.veo.core.usecase.UseCaseSpec
import org.veo.rest.security.NoRestrictionAccessRight

class GetDomainTemplatesUseCaseSpec extends UseCaseSpec {

    DomainTemplateService templateService = Mock()
    ClientRepository clientRepository = Mock()

    GetDomainTemplatesUseCase usecase = new GetDomainTemplatesUseCase(templateService, clientRepository)

    def "retrieve all domaintemplates"() {
        given:
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id

        clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        templateService.getTemplates(existingClient) >> Collections.singletonList(domaintemplate)

        when:
        def output = usecase.execute(new EmptyInput(), noRestrictionExistingClient)

        then:
        output.objects != null
        output.objects.size() == 1
    }

    def "retrieve all domaintemplates unknown client"() {
        given:
        def id = UUID.randomUUID()
        DomainTemplate domaintemplate = Mock()
        domaintemplate.getId() >> id

        def cid = UUID.randomUUID()
        Client client = Mock()
        client.getId() >> cid

        clientRepository.findById(existingClient.id) >> Optional.of(existingClient)
        clientRepository.findById(client.id) >> Optional.empty()
        templateService.getTemplates(existingClient) >> Collections.singletonList(domaintemplate)

        when:
        usecase.execute(new EmptyInput(), NoRestrictionAccessRight.from(cid.toString()))

        then:
        thrown(NotFoundException)
    }
}
