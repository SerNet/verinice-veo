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
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Action;
import org.veo.core.entity.ActionResult;
import org.veo.core.entity.ActionStep;
import org.veo.core.entity.AddRisksStep;
import org.veo.core.entity.ApplyLinkTailoringReferences;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.catalogitem.ApplyCatalogIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogIncarnationDescriptionUseCase;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
  private final EntityFactory factory;

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
                    var risk = ra.obtainRisk(scenario);
                    if (risk.getDesignator() == null) {
                      designatorService.assignDesignator(risk, domain.getOwner());
                      createdEntities.add(risk);
                    }
                  });
        }
      }
      case ApplyLinkTailoringReferences createLinkedScenarios -> {
        Set<Element> controls = createLinkedScenarios.getControls(element, domain);
        controls.forEach(
            control -> {
              var controlCatalogItem = getCatalogItem(domain, control);
              if (controlCatalogItem == null) {
                log.debug(
                    "Control '{}' has no applied catalogItem ... skipping", control.getName());
                return;
              }

              Set<CatalogItem> scenarios =
                  getScenarios(controlCatalogItem, createLinkedScenarios.getLinkType());
              if (scenarios.isEmpty()) {
                log.info(
                    "CatalogItem '{}' has no connections to any scenario.", controlCatalogItem);
                return;
              }

              List<Element> existingScenarios =
                  loadExistingIncarnations(domain, scenarios, element.getOwner());
              var newScenarios =
                  applyIncarnationDescriptionUseCase
                      .execute(
                          new ApplyCatalogIncarnationDescriptionUseCase.InputData(
                              domain.getOwningClient().get(),
                              element.getOwner().getId(),
                              scenarios.stream()
                                  .filter(notIncarnated(existingScenarios))
                                  .map(
                                      item ->
                                          (TemplateItemIncarnationDescriptionState<
                                                  CatalogItem, DomainBase>)
                                              new TemplateItemIncarnationDescription<>(
                                                  item, List.of()))
                                  .toList()))
                      .newElements();
              createdEntities.addAll(newScenarios);
              Stream.concat(existingScenarios.stream(), newScenarios.stream())
                  .forEach(
                      scenario ->
                          createLink(
                              domain, control, scenario, createLinkedScenarios.getLinkType()));
            });
      }
    }
    return new ActionResult(createdEntities);
  }

  private Set<CatalogItem> getScenarios(CatalogItem controlCatalogItem, String linkType) {
    return controlCatalogItem.getTailoringReferences().stream()
        .filter(l -> l.isLinkRef(linkType))
        .map(TailoringReference::getTarget)
        .collect(Collectors.toSet());
  }

  private CatalogItem getCatalogItem(Domain domain, Element control) {
    Optional<CatalogItem> appliedControl =
        control.getAppliedCatalogItems().stream()
            .filter(ci -> ci.requireDomainMembership().equals(domain))
            .findFirst();
    CatalogItem controlCatalogItem = appliedControl.orElse(null);
    return controlCatalogItem;
  }

  private Predicate<? super CatalogItem> notIncarnated(List<Element> existingScenarios) {
    return s -> existingScenarios.stream().noneMatch(es -> es.getAppliedCatalogItems().contains(s));
  }

  private void createLink(Domain domain, Element control, Element scenario, String linkType) {
    control.getLinks(domain).stream()
        .filter(l -> l.getTarget().equals(scenario) && l.getType().equals(linkType))
        .findAny()
        .ifPresentOrElse(
            el -> {
              log.debug("Skip existing link {}", el);
            },
            () -> {
              control.addLink(factory.createCustomLink(scenario, control, linkType, domain));
              genericElementRepository.saveAll(List.of(control));
            });
  }

  private void incarnateScenarios(
      Unit unit,
      Domain domain,
      Set<Entity> createdEntities,
      ApplyLinkTailoringReferences createLinkedScenariosStep,
      Element control,
      CatalogItem controlCatalogItem,
      List<Key<UUID>> scenarioCatalogItems) {

    if (scenarioCatalogItems.isEmpty()) {
      return;
    }
    var config =
        Optional.ofNullable(createLinkedScenariosStep.getConfig())
            .orElse(domain.getIncarnationConfiguration());

    var incarnationDescriptions =
        getIncarnationDescriptionUseCase
            .execute(
                new GetCatalogIncarnationDescriptionUseCase.InputData(
                    unit.getClient(),
                    unit.getId(),
                    domain.getId(),
                    scenarioCatalogItems,
                    config.mode(),
                    config.useExistingIncarnations(),
                    config.include(),
                    config.exclude()))
            .references();

    incarnationDescriptions.stream()
        .forEach(
            icd -> {
              var trp =
                  getReferenceParameterForReference(
                      icd,
                      getTailoringReferences(
                          controlCatalogItem, icd, createLinkedScenariosStep.getLinkType()));

              trp.setReferencedElement(control);
              icd.setReferences(List.of(trp));
            });

    List<Element> newScenario =
        applyIncarnationDescriptionUseCase
            .execute(
                new ApplyCatalogIncarnationDescriptionUseCase.InputData(
                    unit.getClient(),
                    unit.getId(),
                    incarnationDescriptions.stream()
                        .map(
                            r ->
                                (TemplateItemIncarnationDescriptionState<CatalogItem, DomainBase>)
                                    r)
                        .toList()))
            .newElements();

    createdEntities.addAll(newScenario);
  }

  private TailoringReferenceParameter getReferenceParameterForReference(
      TemplateItemIncarnationDescription<CatalogItem, DomainBase> icd,
      TailoringReference<CatalogItem, DomainBase> reference) {
    return icd.getReferences().stream()
        .filter(r -> r.getId().equals(reference.getIdAsString()))
        .findAny()
        .orElseThrow();
  }

  private TailoringReference<CatalogItem, DomainBase> getTailoringReferences(
      CatalogItem controlCatalogItem,
      TemplateItemIncarnationDescription<CatalogItem, DomainBase> incarnationDescription,
      String linkType) {
    return incarnationDescription.getItem().getTailoringReferences().stream()
        .filter(l -> l.isLinkRef(linkType) && l.getTarget().equals(controlCatalogItem))
        .findAny()
        .orElseThrow();
  }

  private List<Element> loadExistingIncarnations(Domain domain, Set<CatalogItem> items, Unit unit) {
    ElementQuery<Element> query = genericElementRepository.query(domain.getOwner());
    query.whereAppliedItemsContain(items);
    query.whereOwnerIs(unit);
    query.fetchAppliedCatalogItems();
    var result = query.execute(PagingConfiguration.UNPAGED);
    return result.getResultPage();
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
