/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.usecase.domaintemplate;

import static org.veo.core.usecase.domaintemplate.DomainTemplateValidator.validateVersion;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

public class CreateDomainTemplateFromDomainUseCase
    implements TransactionalUseCase<
        CreateDomainTemplateFromDomainUseCase.InputData,
        CreateDomainTemplateFromDomainUseCase.OutputData> {
  private final DomainTemplateService domainTemplateService;
  private final DomainRepository repository;
  private final DomainTemplateRepository domainTemplateRepository;

  public CreateDomainTemplateFromDomainUseCase(
      DomainTemplateService domainTemplateService,
      DomainRepository repository,
      DomainTemplateRepository domainTemplateRepository) {
    super();
    this.domainTemplateService = domainTemplateService;
    this.repository = repository;
    this.domainTemplateRepository = domainTemplateRepository;
  }

  @Override
  public OutputData execute(InputData input) {
    Domain domain =
        repository
            .findByIdWithProfilesAndRiskDefinitions(input.id, input.authenticatedClient.getId())
            .orElseThrow(() -> new NotFoundException(input.id, Domain.class));
    Client client = input.authenticatedClient;
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }

    domain = updateVersion(domain, input.version);
    DomainTemplate domainTemplateFromDomain =
        domainTemplateService.createDomainTemplateFromDomain(domain);
    return new OutputData(domainTemplateRepository.save(domainTemplateFromDomain));
  }

  /** Validate and apply new version value. */
  private Domain updateVersion(Domain domain, Version version) {
    validateVersion(version);
    Optional.ofNullable(domain.getDomainTemplate())
        .map(DomainTemplate::getTemplateVersion)
        .map(Version::parse)
        .ifPresentOrElse(
            templateVersion -> {
              if (version.lessThan(templateVersion)) {
                throw new UnprocessableDataException(
                    "Domain template version must be higher than current version %s"
                        .formatted(templateVersion));
              } else if (version.equals(templateVersion)) {
                throw new EntityAlreadyExistsException(
                    "Domain template with version %s already exists".formatted(version));
              }
            },
            () -> {
              domainTemplateRepository
                  .getLatestDomainTemplateId(domain.getName())
                  .ifPresent(
                      (ignored) -> {
                        throw new UnprocessableDataException(
                            "Domain is not based on a template, but templates already exist.");
                      });
            });
    domain.setTemplateVersion(version.toString());
    return repository.save(domain);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(Key<UUID> id, Version version, Client authenticatedClient)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid DomainTemplate newDomainTemplate) implements UseCase.OutputData {}
}
