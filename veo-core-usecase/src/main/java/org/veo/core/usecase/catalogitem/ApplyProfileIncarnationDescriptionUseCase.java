/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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
package org.veo.core.usecase.catalogitem;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.TailoringReferenceTyped;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.parameter.TemplateItemIncarnationDescription;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** Uses a list of {@link IncarnateItemDescription} to create items from a catalog in a unit. */
@Slf4j
public class ApplyProfileIncarnationDescriptionUseCase
    extends AbtractApplyIncarnationDescriptionUseCase<ProfileItem>
    implements TransactionalUseCase<
        ApplyProfileIncarnationDescriptionUseCase.InputData,
        ApplyProfileIncarnationDescriptionUseCase.OutputData> {
  public ApplyProfileIncarnationDescriptionUseCase(
      UnitRepository unitRepository,
      ProfileRepository profileRepository,
      DomainRepository domainRepository,
      RepositoryProvider repositoryProvider,
      DesignatorService designatorService,
      EntityFactory factory) {
    super(designatorService, factory, domainRepository, unitRepository, repositoryProvider);
    this.profileRepository = profileRepository;
  }

  private final ProfileRepository profileRepository;

  @Override
  public OutputData execute(InputData input) {
    // TODO: verinice-veo#2357 refactor this usecase
    log.info("ApplyIncarnationDescriptionUseCase: {}", input);
    Unit unit = unitRepository.getByIdFetchClient(input.getContainerId());
    Client authenticatedClient = input.authenticatedClient;
    unit.checkSameClient(authenticatedClient);
    Map<Key<UUID>, ProfileItem> catalogItemsbyId = catalogItemsById(input.getReferencesToApply());

    checkDomains(input.getAuthenticatedClient(), catalogItemsbyId);

    List<Element> createdElements =
        input.getReferencesToApply().stream()
            .map(ra -> incarnateByDescription(unit, authenticatedClient, catalogItemsbyId, ra))
            .collect(
                Collector.of(
                    IncarnationResult::new,
                    collectElementData(),
                    combineElementData(),
                    elementData -> {
                      processInternalLinks(
                          elementData.getInternalLinks(), elementData.getElements());
                      processParts(elementData.getMapping(), elementData.getInternalLinks());
                      processRisks(elementData.getMapping(), elementData.getInternalLinks());
                      return elementData.getElements();
                    }));
    log.info("ApplyProfileIncarnationDescriptionUseCase elements created: {}", createdElements);
    return new ApplyProfileIncarnationDescriptionUseCase.OutputData(createdElements);
  }

  @Override
  protected ElementResult<ProfileItem> incarnateByDescription(
      Unit unit,
      Client authenticatedClient,
      Map<Key<UUID>, ProfileItem> catalogItemsbyId,
      TemplateItemIncarnationDescription ra) {
    Key<UUID> catalogItemId = ra.getItem().getId();
    ProfileItem catalogItem = catalogItemsbyId.get(catalogItemId);
    if (catalogItem == null) {
      throw new ReferenceTargetNotFoundException(catalogItemId, ProfileItem.class);
    }
    return createElementFromCatalogItem(
        unit,
        authenticatedClient,
        catalogItem,
        catalogItem.getTailoringReferences(),
        catalogItem.requireDomainMembership(),
        ra.getReferences());
  }

  /**
   * Links all {@code resolveInfo} objects with elements created in this batch. Throws an error when
   * the target is not part of the set of created elements.
   */
  @Override
  protected void processInternalLinks(
      List<InternalResolveInfo<ProfileItem>> internalLinks, List<Element> createdCatalogables) {

    internalLinks.stream()
        .filter(TailoringReferenceTyped.IS_ALL_LINK_PREDICATE)
        .forEach(
            ri -> {
              Element internalTarget =
                  createdCatalogables.stream()
                      //                      .filter(c ->
                      // c.getAppliedCatalogItems().contains(ri.getSourceItem()))
                      .findFirst()
                      .orElseThrow(() -> throwInvalidTarget(ri));
              CustomLink link =
                  createLink(ri.source, internalTarget, ri.domain, ri.linkType, ri.attributes);
              ri.source.applyLink(link);
            });
  }

  private void processRisks(
      Map<ProfileItem, Element> mapping, List<InternalResolveInfo<ProfileItem>> internalLinks) {
    mapping
        .entrySet()
        .forEach(
            e -> {
              if (e.getValue() instanceof RiskAffected<?, ?> riskelement) {
                createRisksFor(
                    riskelement,
                    e.getKey().getTailoringReferences().stream()
                        .filter(TailoringReferenceTyped.IS_RISK_PREDICATE)
                        .map(RiskTailoringReference.class::cast)
                        .toList(),
                    e.getKey().requireDomainMembership(),
                    mapping);
              }
            });
  }

  private void createRisksFor(
      RiskAffected<?, ?> riskelement,
      List<RiskTailoringReference> list,
      Domain domain,
      Map<ProfileItem, Element> mapping) {
    list.forEach(
        rr -> {
          if (mapping.get(rr.getTarget()) instanceof Scenario scenario) {
            log.info("create risk for {}, from: {}", riskelement.getName(), rr);
            AbstractRisk<?, ?> risk = riskelement.obtainRisk(scenario, domain);
            if (risk.getDesignator() == null) {
              designatorService.assignDesignator(risk, domain.getOwner());
            }
            if (mapping.get(rr.getRiskOwner()) instanceof Person person) {
              risk.appoint(person);
            }
            if (mapping.get(rr.getMitigation()) instanceof Control control) {
              risk.mitigate(control);
            }
            // TODO: #2387 use the stored risk data
          }
        });
  }

  /** Finds all items given by the ItemdDescription. */
  private Map<Key<UUID>, ProfileItem> catalogItemsById(
      List<TemplateItemIncarnationDescription> referencesToApply) {
    Set<Key<UUID>> catalogItemIds =
        referencesToApply.stream()
            .map(TemplateItemIncarnationDescription::getItem)
            .map(Identifiable::getId)
            .collect(Collectors.toSet());
    Map<Key<UUID>, ProfileItem> catalogItemsbyId =
        profileRepository.findAllByIdsFetchDomainAndTailoringReferences(catalogItemIds).stream()
            .collect(Collectors.toMap(ProfileItem::getId, Function.identity()));
    return catalogItemsbyId;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    Key<UUID> containerId;
    List<TemplateItemIncarnationDescription> referencesToApply;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid List<Element> newElements;
  }
}
