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
import org.veo.core.entity.exception.EntityAlreadyExistsException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateService;
import org.veo.core.usecase.DomainChangeService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDomainTemplateFromDomainUseCase
    implements TransactionalUseCase<
        CreateDomainTemplateFromDomainUseCase.InputData,
        CreateDomainTemplateFromDomainUseCase.OutputData> {
  private final DomainTemplateService domainTemplateService;
  private final DomainRepository repository;
  private final DomainTemplateRepository domainTemplateRepository;
  private final DomainChangeService domainChangeService;

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

    updateVersion(domain, input.version);
    DomainTemplate domainTemplateFromDomain =
        domainTemplateRepository.save(domainTemplateService.createDomainTemplateFromDomain(domain));
    domain.setDomainTemplate(domainTemplateFromDomain);
    return new OutputData(domainTemplateFromDomain);
  }

  /** Validate and apply new version value. */
  private void updateVersion(Domain domain, Version version) {
    validateVersion(version);
    if (domainTemplateRepository.templateExists(domain.getName(), version)) {
      throw new EntityAlreadyExistsException(
          "Domain template %s %s already exists".formatted(domain.getName(), version));
    }
    var introducesBreakingChanges =
        domainChangeService.evaluateChanges(domain).introducesBreakingChanges();
    Optional.ofNullable(domain.getDomainTemplate())
        .map(DomainTemplate::getTemplateVersion)
        .map(Version::parse)
        .ifPresentOrElse(
            /*
             If the domain is based on an existing template, enforce restrictions for different types of updates:
             * Major and minor versions may only be created from a domain based on the latest template.
             * Patches can also be created as hotfixes for outdated templates.
             * Version numbers must not be skipped.
             * Only major versions may contain breaking changes.
             * Patch versions must not contain any structural changes.
            */
            templateVersion -> {
              switch (getUpdateType(templateVersion, version)) {
                case UpdateType.MAJOR -> {
                  validateNewMinorOrMajor(domain, templateVersion);
                }
                case UpdateType.MINOR -> {
                  validateNewMinorOrMajor(domain, templateVersion);
                  if (introducesBreakingChanges) {
                    throwMustBeMajor(templateVersion);
                  }
                }
                case UpdateType.PATCH -> {
                  if (introducesBreakingChanges) {
                    throwMustBeMajor(templateVersion);
                  }
                  // TODO #3315 forbid any structural changes
                }
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
  }

  private static void throwMustBeMajor(Version templateVersion) {
    throw new UnprocessableDataException(
        "Domain contains breaking changes and must be released as a major (%s) update."
            .formatted(templateVersion.nextMajorVersion()));
  }

  private void validateNewMinorOrMajor(Domain domain, Version templateVersion) {
    var latest = domainTemplateRepository.getLatestVersion(domain.getName());
    if (!templateVersion.equals(latest)) {
      throw new UnprocessableDataException(
          "Given domain is based on version %s, but a new minor or major version can only be created from the latest template %s."
              .formatted(templateVersion, latest));
    }
  }

  private UpdateType getUpdateType(Version oldVersion, Version newVersion) {
    var nextPatch = oldVersion.nextPatchVersion();
    var nextMinor = oldVersion.nextMinorVersion();
    var nextMajor = oldVersion.nextMajorVersion();

    if (newVersion.equals(nextPatch)) {
      return UpdateType.PATCH;
    }
    if (newVersion.equals(nextMinor)) {
      return UpdateType.MINOR;
    }
    if (newVersion.equals(nextMajor)) {
      return UpdateType.MAJOR;
    }
    throw new UnprocessableDataException(
        "Unexpected version - expected next patch (%s), minor (%s) or major (%s)."
            .formatted(nextPatch, nextMinor, nextMajor));
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(UUID id, Version version, Client authenticatedClient)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid DomainTemplate newDomainTemplate) implements UseCase.OutputData {}
}
