/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.usecase.service;

import static org.veo.core.entity.risk.DomainRiskReferenceProvider.referencesForDomain;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.ProcessRiskValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.ScenarioRiskValues;
import org.veo.core.entity.state.CompositeElementState;
import org.veo.core.entity.state.ControlDomainAssociationState;
import org.veo.core.entity.state.ControlRiskValuesState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.state.ProcessDomainAssociationState;
import org.veo.core.entity.state.ScenarioDomainAssociationState;
import org.veo.core.entity.state.ScopeDomainAssociationState;
import org.veo.core.entity.state.ScopeState;
import org.veo.core.entity.state.UnitState;
import org.veo.core.entity.transform.EntityFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityStateMapper {

  private final EntityFactory entityFactory;

  public void mapState(UnitState source, Unit target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);

    target.setDomains(idRefResolver.resolve(source.getDomains()));
    target.setParent(
        Optional.ofNullable(source.getParent()).map(idRefResolver::resolve).orElse(null));
  }

  private static void mapNameableProperties(Nameable source, Nameable target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }

  public <T extends Element> void mapState(
      ElementState<T> source, T target, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomains(), target, idRefResolver);
    mapElement(source, target, idRefResolver);
    if (target instanceof Scope scope) {
      ScopeState scopeState = (ScopeState) source;
      Set<? extends ITypedId<Element>> memberReferences = scopeState.getMembers();
      Set<Element> members =
          memberReferences.stream()
              .collect(
                  Collectors.groupingBy(ITypedId::getType, Collectors.<ITypedId<Element>>toSet()))
              .values()
              .stream()
              .flatMap(refs -> idRefResolver.resolve(refs).stream())
              .collect(Collectors.toSet());

      scope.setMembers(members);
    }
    if (target instanceof CompositeElement ce) {
      CompositeElementState<T> compositeElementState = (CompositeElementState<T>) source;
      ce.setParts(idRefResolver.resolve(compositeElementState.getParts()));
    }
  }

  private <T extends Element> void mapElement(
      ElementState<T> source, T target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);

    applyLinks(source, target, idRefResolver);
    applyCustomAspects(source, target, idRefResolver);

    if (source.getOwner() != null) {
      target.setOwnerOrContainingCatalogItem(idRefResolver.resolve(source.getOwner()));
    }
  }

  private <T extends Element> void applyLinks(
      ElementState<T> source, T target, IdRefResolver idRefResolver) {
    var newLinks = source.getCustomLinkStates();
    // Remove old links that are absent in new links
    target.getLinks().stream()
        .filter(
            oldLink ->
                newLinks.stream()
                    .noneMatch(
                        newLink ->
                            newLink.getType().equals(oldLink.getType())
                                && oldLink
                                    .getTarget()
                                    .getIdAsString()
                                    .equals(newLink.getTarget().getId())))
        .forEach(target::removeLink);
    // Apply new links
    newLinks.forEach(
        link -> {
          CustomLink newLink =
              entityFactory.createCustomLink(
                  idRefResolver.resolve(link.getTarget()),
                  target,
                  link.getType(),
                  idRefResolver.resolve(getCustomAspectOrLinkDomain(source)));

          target.applyLink(newLink);
        });
  }

  private <T extends Element> void applyCustomAspects(
      ElementState<T> source, T target, IdRefResolver idRefResolver) {
    var customAspectStates = source.getCustomAspectStates();
    // Remove old CAs that are absent in new CAs
    target.getCustomAspects().stream()
        .filter(
            oldCa ->
                customAspectStates.stream()
                    .noneMatch(newCa -> newCa.getType().equals(oldCa.getType())))
        .forEach(target::removeCustomAspect);
    // Apply new CAs
    customAspectStates.forEach(
        caState -> {
          var newCa =
              entityFactory.createCustomAspect(
                  caState.getType(), idRefResolver.resolve(getCustomAspectOrLinkDomain(source)));
          newCa.setAttributes(caState.getAttributes());
          target.applyCustomAspect(newCa);
        });
  }

  private static ITypedId<Domain> getCustomAspectOrLinkDomain(ElementState<?> source) {
    Set<String> domains = source.getDomains().keySet();
    if (domains.size() == 1) {
      return TypedId.from(domains.iterator().next(), Domain.class);
    }
    if (domains.isEmpty()) {
      throw new IllegalArgumentException(
          "Element cannot contain custom aspects or links without being associated with a domain");
    }
    throw new UnprocessableDataException(
        "Using custom aspects or links in a multi-domain element is not supported by this API");
  }

  private void mapToEntity(
      Map<String, ? extends DomainAssociationState> domains,
      Element target,
      IdRefResolver idRefResolver) {
    BiConsumer<DomainBase, DomainAssociationState> customMapper = (domain, association) -> {};

    if (target instanceof Process process) {
      customMapper =
          (domain, association) ->
              process.setImpactValues(
                  domain,
                  mapImpactValues(
                      ((ProcessDomainAssociationState) association).getRiskValues(), domain));
    } else if (target instanceof Scope scope) {
      customMapper =
          (domain, association) ->
              scope.setRiskDefinition(
                  domain,
                  toRiskDefinitionRef(
                      ((ScopeDomainAssociationState) association).getRiskDefinition(), domain));
    } else if (target instanceof Control control) {
      customMapper =
          (domain, association) ->
              control.setRiskValues(
                  domain,
                  mapRiskValues(
                      ((ControlDomainAssociationState) association).getRiskValues(), domain));
    } else if (target instanceof Scenario scenario) {
      customMapper =
          (domain, association) ->
              scenario.setPotentialProbability(
                  domain,
                  mapPotentialProbability(
                      ((ScenarioDomainAssociationState) association).getRiskValues(), domain));
    }

    mapToEntity(domains, target, idRefResolver, customMapper);
  }

  private Map<RiskDefinitionRef, PotentialProbabilityImpl> mapPotentialProbability(
      Map<String, ? extends ScenarioRiskValues> riskValues, DomainBase domain) {
    return riskValues.entrySet().stream().collect(groupScenarioRiskValuesByDomain(domain));
  }

  private Collector<
          Map.Entry<String, ? extends ScenarioRiskValues>,
          ?,
          Map<RiskDefinitionRef, PotentialProbabilityImpl>>
      groupScenarioRiskValuesByDomain(DomainBase domain) {
    var referenceProvider = referencesForDomain(domain);
    return Collectors.toMap(
        kv -> toRiskDefinitionRef(kv.getKey(), domain),
        kv -> mapScenarioRiskValuesDto2Entity(kv.getKey(), kv.getValue(), referenceProvider));
  }

  private PotentialProbabilityImpl mapScenarioRiskValuesDto2Entity(
      String riskDefinitionId,
      ScenarioRiskValues riskValuesDto,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new PotentialProbabilityImpl();

    riskValues.setPotentialProbability(
        Optional.ofNullable(riskValuesDto.getPotentialProbability())
            .map(pp -> referenceProvider.getProbabilityRef(riskDefinitionId, pp))
            .orElse(null));
    riskValues.setPotentialProbabilityExplanation(
        riskValuesDto.getPotentialProbabilityExplanation());
    return riskValues;
  }

  private Map<RiskDefinitionRef, ControlRiskValues> mapRiskValues(
      Map<String, ? extends ControlRiskValuesState> riskValues, DomainBase domain) {

    var referenceProvider = referencesForDomain(domain);
    return riskValues.entrySet().stream()
        .collect(
            Collectors.toMap(
                kv -> referenceProvider.getRiskDefinitionRef(kv.getKey()),
                kv -> mapControlRiskValues2Entity(kv.getKey(), kv.getValue(), referenceProvider)));
  }

  private ControlRiskValues mapControlRiskValues2Entity(
      String riskDefinitionId,
      ControlRiskValuesState riskValuesState,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new ControlRiskValues();
    riskValues.setImplementationStatus(
        Optional.ofNullable(riskValuesState.getImplementationStatus())
            .map(status -> referenceProvider.getImplementationStatus(riskDefinitionId, status))
            .orElse(null));
    return riskValues;
  }

  private void mapToEntity(
      Map<String, ? extends DomainAssociationState> domains,
      Element target,
      IdRefResolver idRefResolver,
      BiConsumer<DomainBase, DomainAssociationState> customMapper) {
    domains.forEach(
        (domainId, association) -> {
          DomainBase domain = idRefResolver.resolve(domainId, Domain.class);
          String newSubType = association.getSubType();
          String newStatus = association.getStatus();
          target
              .findSubType(domain)
              .ifPresentOrElse(
                  oldSubType -> {
                    if (!newSubType.equals(oldSubType)) {
                      throw new IllegalArgumentException(
                          "Cannot change a sub type on an existing element");
                    }
                    target.setStatus(newStatus, (Domain) domain);
                  },
                  () -> target.associateWithDomain(domain, newSubType, association.getStatus()));

          customMapper.accept(domain, association);
        });
  }

  private Map<RiskDefinitionRef, ProcessImpactValues> mapImpactValues(
      Map<String, ? extends ProcessRiskValues> riskValues, DomainBase domain) {
    var referenceProvider = referencesForDomain(domain);
    return riskValues.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> referenceProvider.getRiskDefinitionRef(e.getKey()),
                e -> mapProcessImpactValues(e.getKey(), e.getValue(), referenceProvider)));
  }

  private ProcessImpactValues mapProcessImpactValues(
      String riskDefinitionId,
      ProcessRiskValues value,
      DomainRiskReferenceProvider referenceProvider) {
    var riskValues = new ProcessImpactValues();

    Map<CategoryRef, ImpactRef> potentialImpacts =
        value.getPotentialImpacts().entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> toCategoryRef(riskDefinitionId, referenceProvider, e), Entry::getValue));
    riskValues.setPotentialImpacts(potentialImpacts);
    return riskValues;
  }

  private CategoryRef toCategoryRef(
      String riskDefinitionId,
      DomainRiskReferenceProvider referenceProvider,
      Entry<String, ImpactRef> e) {
    return referenceProvider.getCategoryRef(riskDefinitionId, e.getKey());
  }

  RiskDefinitionRef toRiskDefinitionRef(String riskDefId, DomainBase domain) {
    if (riskDefId == null) {
      return null;
    }
    return referencesForDomain(domain).getRiskDefinitionRef(riskDefId);
  }
}
