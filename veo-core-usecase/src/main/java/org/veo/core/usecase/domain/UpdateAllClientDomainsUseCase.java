/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.core.usecase.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;
import org.veo.service.ElementMigrationService;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class UpdateAllClientDomainsUseCase
        implements TransactionalUseCase<UpdateAllClientDomainsUseCase.InputData, EmptyOutput> {

    private final DomainRepository domainRepository;
    private final RepositoryProvider repositoryProvider;
    private final UnitRepository unitRepository;
    private final ElementMigrationService elementMigrationService;

    @Override
    public EmptyOutput execute(InputData input) {
        Set<Domain> newDomains = domainRepository.findAllByTemplateId(input.domainTemplateId);
        for (Domain newDomain : newDomains) {
            Client client = newDomain.getOwner();

            Set<Domain> clientActiveDomains = client.getDomains()
                                                    .stream()
                                                    .filter(Domain::isActive)
                                                    .filter(d -> d.getName()
                                                                  .equals(newDomain.getName()))
                                                    .collect(Collectors.toSet());
            if (clientActiveDomains.size() != 2) {
                log.warn("Skipping client {}, found {} active domains instead of 2", client,
                         clientActiveDomains.size());
                continue;
            }
            Domain domainToUpdate = clientActiveDomains.stream()
                                                       .filter(Predicate.not(newDomain::equals))
                                                       .findAny()
                                                       .orElseThrow();
            performMigration(client, domainToUpdate, newDomain);
            domainToUpdate.setActive(false);
        }

        return EmptyOutput.INSTANCE;
    }

    private void performMigration(Client client, Domain domainToUpdate, Domain newDomain) {
        log.info("Performing migration for domain {}->{}", domainToUpdate, newDomain);
        List<Unit> unitsToUpdate = unitRepository.findByClient(client);
        for (Unit unit : unitsToUpdate) {
            if (unit.removeFromDomains(domainToUpdate)) {
                unit.addToDomains(newDomain);
            }
        }
        EntityType.ELEMENT_TYPES.stream()
                                .forEach(type -> {
                                    Set<? extends Element> elements = repositoryProvider.getElementRepositoryFor((Class<Element>) type.getType())
                                                                                        .findByDomain(domainToUpdate);
                                    elements.forEach(element -> {
                                        element.transferToDomain(domainToUpdate, newDomain);
                                        elementMigrationService.migrate(element,
                                                                        newDomain.getElementTypeDefinition(type.getSingularTerm())
                                                                                 .orElseThrow(),
                                                                        newDomain);
                                    });
                                });
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Key<UUID> domainTemplateId;
    }
}
