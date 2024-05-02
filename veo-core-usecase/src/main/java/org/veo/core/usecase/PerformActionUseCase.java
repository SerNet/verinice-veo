/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.usecase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Action;
import org.veo.core.entity.ActionResult;
import org.veo.core.entity.ActionStep;
import org.veo.core.entity.AddRisksStep;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.Key;
import org.veo.core.entity.ReapplyCatalogItemsStep;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.usecase.catalogitem.ApplyCatalogIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogIncarnationDescriptionUseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PerformActionUseCase
    implements TransactionalUseCase<
        PerformActionUseCase.InputData, PerformActionUseCase.OutputData> {
  private final ClientRepository clientRepository;
  private final DomainRepository domainRepository;
  private final GenericElementRepository genericElementRepository;
  private final GetCatalogIncarnationDescriptionUseCase getIncarnationDescriptionUseCase;
  private final ApplyCatalogIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase;
  private final DesignatorService designatorService;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public OutputData execute(InputData input) {
    var client = clientRepository.getActiveById(input.clientId);
    var domain = domainRepository.getActiveById(input.domainId, input.clientId);
    var action =
        domain
            .findAction(input.actionId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Action %s not found in domain %s"
                            .formatted(input.actionId, domain.getIdAsString())));
    var element = genericElementRepository.getById(input.elementId, input.elementType, client);
    return new OutputData(perform(action, element, domain));
  }

  private ActionResult perform(Action action, Element element, Domain domain) {
    if (!action.elementTypes().contains(element.getModelType())) {
      throw new UnprocessableDataException(
          "Action cannot be performed on %s - must be one of %s"
              .formatted(element.getModelType(), String.join(",", action.elementTypes())));
    }
    return action.steps().stream()
        .map(s -> perform(s, element, domain))
        .reduce(
            new ActionResult(Collections.emptySet()),
            (a, b) ->
                new ActionResult(
                    Stream.concat(a.createdEntities().stream(), b.createdEntities().stream())
                        .collect(Collectors.toSet())));
  }

  private ActionResult perform(ActionStep step, Element element, Domain domain) {
    var createdEntities = new HashSet<Entity>();
    switch (step) {
      case AddRisksStep createRisksStep -> {
        if (element instanceof RiskAffected<?, ?> ra) {
          createRisksStep
              .getScenarios(element, domain)
              .forEach(
                  scenario -> {
                    var risk = ra.obtainRisk(scenario, domain);
                    if (risk.getDesignator() == null) {
                      designatorService.assignDesignator(risk, domain.getOwner());
                      createdEntities.add(risk);
                    }
                  });
        }
      }
      case ReapplyCatalogItemsStep reapplyCatalogItemsStep -> {
        var catalogItemIds =
            reapplyCatalogItemsStep.getIncarnations(element, domain).stream()
                .flatMap(c -> c.getAppliedCatalogItems().stream())
                .filter(ci -> ci.requireDomainMembership().equals(domain))
                .map(SymIdentifiable::getSymbolicId)
                .distinct()
                .toList();
        createdEntities.addAll(
            reapplyCatalogItems(
                catalogItemIds,
                element.getOwner(),
                domain,
                Optional.ofNullable(reapplyCatalogItemsStep.getConfig())
                    .orElse(domain.getIncarnationConfiguration())));
      }
    }
    return new ActionResult(createdEntities);
  }

  private List<Element> reapplyCatalogItems(
      List<Key<UUID>> catalogItemIds, Unit unit, Domain domain, IncarnationConfiguration config) {
    // TODO #852 pass "re-apply" flag
    var incarnationDescriptions =
        getIncarnationDescriptionUseCase
            .execute(
                new GetCatalogIncarnationDescriptionUseCase.InputData(
                    unit.getClient(),
                    unit.getId(),
                    domain.getId(),
                    catalogItemIds,
                    config.mode(),
                    config.useExistingIncarnations(),
                    config.include(),
                    config.exclude()))
            .getReferences()
            .stream()
            .map(r -> (TemplateItemIncarnationDescriptionState<CatalogItem, DomainBase>) r)
            .toList();
    return applyIncarnationDescriptionUseCase
        .execute(
            new ApplyCatalogIncarnationDescriptionUseCase.InputData(
                unit.getClient(), unit.getId(), incarnationDescriptions))
        .getNewElements();
  }

  public record InputData(
      @NotNull Key<UUID> domainId,
      @NotNull Key<UUID> elementId,
      @NotNull Class<? extends Element> elementType,
      @NotNull String actionId,
      @NotNull Key<UUID> clientId)
      implements UseCase.InputData {}

  public record OutputData(@NotNull ActionResult result) implements UseCase.OutputData {}
}
