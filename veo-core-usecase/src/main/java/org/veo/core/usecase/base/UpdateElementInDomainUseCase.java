/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.base;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.common.ETagMismatchException;
import org.veo.core.usecase.decision.Decider;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public abstract class UpdateElementInDomainUseCase<T extends Element>
    implements TransactionalUseCase<
        UpdateElementInDomainUseCase.InputData<T>, UpdateElementInDomainUseCase.OutputData<T>> {

  private final DomainRepository domainRepository;
  private final ElementRepository<T> repo;
  private final Decider decider;

  @Override
  public OutputData<T> execute(InputData<T> input) {
    var domain = domainRepository.getById(input.getDomainId());
    var inputElement = input.getElement();
    var storedElement = repo.getById(input.element.getId(), input.authenticatedClient.getId());
    storedElement.checkSameClient(input.authenticatedClient); // Client boundary safety net
    if (!storedElement.isAssociatedWithDomain(domain)) {
      throw new NotFoundException(
          "%s %s is not associated with domain %s",
          storedElement.getModelInterface().getSimpleName(),
          storedElement.getIdAsString(),
          domain.getIdAsString());
    }
    checkETag(storedElement, input);
    applyChanges(inputElement, storedElement, domain);
    storedElement.setDecisionResults(decider.decide(storedElement, domain), domain);
    DomainSensitiveElementValidator.validate(inputElement);
    repo.save(storedElement);
    // re-fetch element to make sure it is returned with updated versioning information
    return new OutputData<>(repo.getById(storedElement.getId(), input.authenticatedClient.getId()));
  }

  protected void applyChanges(T source, T target, Domain domain) {
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setAbbreviation(source.getAbbreviation());
    target.setOwner(source.getOwner());

    target.setStatus(source.getStatus(domain), domain);
    applyCustomAspects(source, target, domain);
    applyLinks(source, target, domain);

    // TODO VEO-1874: Only mark root element as updated when basic properties change, version domain
    // associations independently.
    target.setUpdatedAt(Instant.now());
  }

  private void applyLinks(T source, T target, Domain domain) {
    var newLinks = source.getLinks(domain);
    // Remove old links that are absent in new links
    target.getLinks(domain).stream()
        .filter(
            oldLink ->
                newLinks.stream()
                    .noneMatch(
                        newLink ->
                            newLink.getType().equals(oldLink.getType())
                                && newLink.getTarget().equals(oldLink.getTarget())))
        .forEach(target::removeLink);
    // Apply new links
    newLinks.forEach(target::applyLink);
  }

  private void applyCustomAspects(T source, T target, Domain domain) {
    var newCas = source.getCustomAspects(domain);
    // Remove old CAs that are absent in new CAs
    target.getCustomAspects(domain).stream()
        .filter(
            oldCa -> newCas.stream().noneMatch(newCa -> newCa.getType().equals(oldCa.getType())))
        .forEach(target::removeCustomAspect);
    // Apply new CAs
    newCas.forEach(target::applyCustomAspect);
  }

  private void checkETag(Element storedElement, InputData<? extends Element> input) {
    if (!ETag.matches(
        storedElement.getId().uuidValue(), storedElement.getVersion(), input.getETag())) {
      throw new ETagMismatchException(
          String.format(
              "The eTag does not match for the element with the ID %s",
              storedElement.getId().uuidValue()));
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData<T> implements UseCase.InputData {
    @Valid T element;
    Key<UUID> domainId;
    Client authenticatedClient;
    String eTag;
    String username;
  }

  @Valid
  @Value
  public static class OutputData<T> implements UseCase.OutputData {
    @Valid T entity;
  }
}
