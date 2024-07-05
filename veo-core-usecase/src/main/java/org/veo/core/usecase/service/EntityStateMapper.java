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

import static java.util.HashMap.newHashMap;
import static org.veo.core.entity.EntityType.RISK_AFFECTED_TYPES;
import static org.veo.core.entity.EntityType.getBySingularTerm;
import static org.veo.core.entity.EntityType.getSingularTermByType;
import static org.veo.core.entity.risk.DomainRiskReferenceProvider.referencesForDomain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.event.ControlPartsChangedEvent;
import org.veo.core.entity.event.RiskAffectedLinkDeletedEvent;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.ScenarioRiskValues;
import org.veo.core.entity.state.CompositeElementState;
import org.veo.core.entity.state.ControlDomainAssociationState;
import org.veo.core.entity.state.ControlImplementationState;
import org.veo.core.entity.state.ControlRiskValuesState;
import org.veo.core.entity.state.CustomLinkState;
import org.veo.core.entity.state.DomainAssociationState;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.state.PotentialImpactDomainAssociationState;
import org.veo.core.entity.state.RequirementImplementationState;
import org.veo.core.entity.state.RiskAffectedState;
import org.veo.core.entity.state.RiskState;
import org.veo.core.entity.state.ScenarioDomainAssociationState;
import org.veo.core.entity.state.ScopeDomainAssociationState;
import org.veo.core.entity.state.ScopeState;
import org.veo.core.entity.state.UnitState;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.service.EventPublisher;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityStateMapper {

  private final EntityFactory entityFactory;

  private final EventPublisher eventPublisher;

  private EntityStateMapper() {
    entityFactory = null;
    eventPublisher = null;
  }

  public void mapState(UnitState source, Unit target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);

    target.setDomains(idRefResolver.resolve(source.getDomains()));
    target.setParent(
        Optional.ofNullable(source.getParent()).map(idRefResolver::resolve).orElse(null));
  }

  public static void mapNameableProperties(Nameable source, Nameable target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }

  public <T extends Element, S extends ElementState<T>> void mapState(
      S source, T target, boolean removeFromOtherDomains, IdRefResolver idRefResolver) {
    mapToEntity(source.getDomainAssociationStates(), target, idRefResolver, removeFromOtherDomains);
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
      var oldParts = ce.getPartsRecursively();
      ce.setParts(idRefResolver.resolve(compositeElementState.getParts()));
      publishPartsChanged(ce, oldParts);
    }
  }

  private <T extends CompositeElement<T>> void ok(T ce) {}

  public <R extends AbstractRisk<T, R>, T extends RiskAffected<T, R>> R mapState(
      RiskState<R, T> source, DbIdRefResolver resolver) {
    var element = resolver.resolve(source.getOwnerRef());
    var scenario = resolver.resolve(source.getScenarioRef());
    var domains = resolver.resolve(source.getDomainRefs());
    var target = element.obtainRisk(scenario, domains);
    target.appoint(
        Optional.ofNullable(source.getRiskOwnerRef()).map(resolver::resolve).orElse(null));
    target.mitigate(
        Optional.ofNullable(source.getMitigationRef()).map(resolver::resolve).orElse(null));
    target.defineRiskValues(source.getRiskValues());
    return target;
  }

  public void mapState(
      RequirementImplementationState source,
      RequirementImplementation target,
      IdRefResolver idRefResolver) {
    target.setImplementationStatement(source.getImplementationStatement());
    target.setResponsible(
        Optional.ofNullable(source.getResponsible()).map(idRefResolver::resolve).orElse(null));
    target.setOrigination(source.getOrigination());
    target.setStatus(source.getStatus());
    target.setImplementationUntil(
        Optional.ofNullable(source.getImplementationUntil()).map(LocalDate::parse).orElse(null));
  }

  private void publishPartsChanged(CompositeElement entity, Set oldParts) {
    // So far we only publish changes for control parts.
    // Do not publish if entity has no client.
    if (entity instanceof Control control && entity.getOwningClient().isPresent()) {
      eventPublisher.publish(new ControlPartsChangedEvent(control, oldParts));
    }
  }

  private void publishLinkRemoved(Domain domain, Element target, String type) {
    if (target.getOwningClient().isPresent()
        && RISK_AFFECTED_TYPES.contains(
            getBySingularTerm(getSingularTermByType(target.getModelInterface())))) {
      eventPublisher.publish(new RiskAffectedLinkDeletedEvent(target, domain, type, this));
    }
  }

  /** Maps link state to link entity (without adding the link to the source element). */
  public CustomLink mapLink(
      CustomLinkState link, Element source, Domain domain, IdRefResolver idRefResolver) {
    CustomLink newLink =
        entityFactory.createCustomLink(
            idRefResolver.resolve(link.getTarget()), source, link.getType(), domain);
    newLink.setAttributes(link.getAttributes());
    return newLink;
  }

  private <T extends Element> void mapElement(
      ElementState<T> source, T target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);

    if (source.getOwner() != null) {
      target.setOwner(idRefResolver.resolve(source.getOwner()));
    }

    if (source instanceof RiskAffectedState<?> sourceRa
        && target instanceof RiskAffected<?, ?> targetRa) {
      applyControlImplementations(sourceRa, targetRa, idRefResolver);
    }
  }

  private static <T extends Element> void applyControlImplementations(
      RiskAffectedState<?> source, RiskAffected<?, ?> target, IdRefResolver idRefResolver) {

    // Remove old CIs that are absent in list of new CIs
    target.getControlImplementations().stream()
        .map(ControlImplementation::getControl)
        .filter(isNotPresentIn(source))
        .forEach(target::disassociateControl);

    // Apply new CIs
    Set<ControlImplementationState> states = source.getControlImplementationStates();
    states.forEach(ciState -> transferState(target, idRefResolver, ciState));
  }

  private static void transferState(
      RiskAffected<?, ?> target,
      IdRefResolver idRefResolver,
      ControlImplementationState sourceState) {
    var ci = target.implementControl(idRefResolver.resolve(sourceState.getControl()));
    ci.setDescription(sourceState.getDescription());
    ci.setResponsible(
        Optional.ofNullable(sourceState.getResponsible()).map(idRefResolver::resolve).orElse(null));
  }

  private static Predicate<Control> isNotPresentIn(RiskAffectedState<?> source) {
    return ctl ->
        source.getControlImplementationStates().stream()
            .noneMatch(ciState -> ciState.references(ctl));
  }

  private <T extends Element> void applyLinks(
      DomainAssociationState source, T target, IdRefResolver idRefResolver, Domain domain) {
    var newLinks = source.getCustomLinkStates();
    // Remove old links that are absent in new links
    Set<CustomLink> removedLinks =
        Set.copyOf(target.getLinks(domain)).stream()
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
            .collect(Collectors.toSet());
    removedLinks.forEach(target::removeLink);
    // Apply new links
    newLinks.forEach(
        link -> {
          CustomLink newLink = mapLink(link, target, domain, idRefResolver);
          target.applyLink(newLink);
        });

    removedLinks.forEach(rl -> publishLinkRemoved(domain, rl.getTarget(), rl.getType()));
  }

  private <T extends Element> void applyCustomAspects(
      DomainAssociationState source, T target, Domain domain) {

    var customAspectStates = source.getCustomAspectStates();
    // Remove old CAs that are absent in new CAs
    Set.copyOf(target.getCustomAspects(domain)).stream()
        .filter(
            oldCa ->
                customAspectStates.stream()
                    .noneMatch(newCa -> newCa.getType().equals(oldCa.getType())))
        .forEach(target::removeCustomAspect);
    // Apply new CAs
    customAspectStates.forEach(
        caState -> {
          var newCa = entityFactory.createCustomAspect(caState.getType(), domain);
          newCa.setAttributes(caState.getAttributes());
          target.applyCustomAspect(newCa);
        });
  }

  private void mapToEntity(
      Set<? extends DomainAssociationState> domains,
      Element target,
      IdRefResolver idRefResolver,
      boolean removeFromOtherDomains) {
    BiConsumer<Domain, DomainAssociationState> customMapper = (domain, association) -> {};

    if (target instanceof Process process) {
      customMapper =
          (domain, association) ->
              process.setImpactValues(
                  domain,
                  mapImpactValues(
                      ((PotentialImpactDomainAssociationState) association).getRiskValues(),
                      domain,
                      process.getImpactValues(domain)));
    } else if (target instanceof Asset asset) {
      customMapper =
          (domain, association) ->
              asset.setImpactValues(
                  domain,
                  mapImpactValues(
                      ((PotentialImpactDomainAssociationState) association).getRiskValues(),
                      domain,
                      asset.getImpactValues(domain)));
    } else if (target instanceof Scope scope) {
      customMapper =
          (domain, association) -> {
            scope.setRiskDefinition(
                domain,
                toRiskDefinitionRef(
                    ((ScopeDomainAssociationState) association).getRiskDefinition(), domain));
            scope.setImpactValues(
                domain,
                mapImpactValues(
                    ((ScopeDomainAssociationState) association).getRiskValues(),
                    domain,
                    scope.getImpactValues(domain)));
          };
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

    mapToEntity(domains, target, idRefResolver, customMapper, removeFromOtherDomains);
  }

  private Map<RiskDefinitionRef, PotentialProbability> mapPotentialProbability(
      Map<String, ? extends ScenarioRiskValues> riskValues, Domain domain) {
    return riskValues.entrySet().stream().collect(groupScenarioRiskValuesByDomain(domain));
  }

  private Collector<
          Map.Entry<String, ? extends ScenarioRiskValues>,
          ?,
          Map<RiskDefinitionRef, PotentialProbability>>
      groupScenarioRiskValuesByDomain(Domain domain) {
    var referenceProvider = referencesForDomain(domain);
    return Collectors.toMap(
        kv -> toRiskDefinitionRef(kv.getKey(), domain),
        kv -> mapScenarioRiskValuesDto2Entity(kv.getKey(), kv.getValue(), referenceProvider));
  }

  private PotentialProbability mapScenarioRiskValuesDto2Entity(
      String riskDefinitionId,
      ScenarioRiskValues riskValuesDto,
      DomainRiskReferenceProvider referenceProvider) {
    return new PotentialProbability(
        Optional.ofNullable(riskValuesDto.getPotentialProbability())
            .map(pp -> referenceProvider.getProbabilityRef(riskDefinitionId, pp))
            .orElse(null),
        riskValuesDto.getPotentialProbabilityExplanation());
  }

  private Map<RiskDefinitionRef, ControlRiskValues> mapRiskValues(
      Map<String, ? extends ControlRiskValuesState> riskValues, Domain domain) {

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
    return new ControlRiskValues(
        Optional.ofNullable(riskValuesState.getImplementationStatus())
            .map(status -> referenceProvider.getImplementationStatus(riskDefinitionId, status))
            .orElse(null));
  }

  private <T extends Element> void mapToEntity(
      Set<? extends DomainAssociationState> domains,
      T target,
      IdRefResolver idRefResolver,
      BiConsumer<Domain, DomainAssociationState> customMapper,
      boolean removeFromOtherDomains) {
    if (removeFromOtherDomains) {
      target.getDomains().stream()
          .filter(
              d -> domains.stream().noneMatch(a -> a.getDomain().getId().equals(d.getIdAsString())))
          .forEach(target::removeFromDomains);
    }
    domains.forEach(
        association -> {
          Domain domain = idRefResolver.resolve(association.getDomain());
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
                    target.setStatus(newStatus, domain);
                  },
                  () -> target.associateWithDomain(domain, newSubType, association.getStatus()));
          applyLinks(association, target, idRefResolver, domain);
          applyCustomAspects(association, target, domain);
          customMapper.accept(domain, association);
        });
  }

  private Map<RiskDefinitionRef, ImpactValues> mapImpactValues(
      Map<String, ? extends PotentialImpactValues> riskValues,
      Domain domain,
      Map<RiskDefinitionRef, ImpactValues> currentImpactValues) {
    if (riskValues == null || riskValues.isEmpty()) {
      return newHashMap(5);
    }
    var referenceProvider = referencesForDomain(domain);
    return riskValues.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> referenceProvider.getRiskDefinitionRef(e.getKey()),
                e ->
                    mapImpactValues(
                        e.getKey(),
                        e.getValue(),
                        currentImpactValues.getOrDefault(
                            referenceProvider.getRiskDefinitionRef(e.getKey()),
                            new ImpactValues(new HashMap<>())),
                        referenceProvider)));
  }

  private ImpactValues mapImpactValues(
      String riskDefinitionId,
      PotentialImpactValues values,
      ImpactValues currentValues, // TODO #2663 remove
      DomainRiskReferenceProvider referenceProvider) {
    // TODO #2663 Remove this automatism (it is only needed as long as the frontend cannot manage
    // the new impact maps correctly).
    // For all removed potential impacts values, automatically remove the corresponding reason and
    // explanation as well.
    currentValues.potentialImpacts().keySet().stream()
        .map(CategoryRef::getIdRef)
        .filter(cat -> !values.getPotentialImpacts().containsKey(cat))
        .forEach(
            removedCat -> {
              values.getPotentialImpactReasons().remove(removedCat);
              values.getPotentialImpactExplanations().remove(removedCat);
            });
    return new ImpactValues(
        mapToCategories(values.getPotentialImpacts(), riskDefinitionId, referenceProvider),
        mapToCategories(
            values.getPotentialImpactsCalculated(), riskDefinitionId, referenceProvider),
        mapToCategories(values.getPotentialImpactReasons(), riskDefinitionId, referenceProvider),
        mapToCategories(
            values.getPotentialImpactExplanations(), riskDefinitionId, referenceProvider));
  }

  /**
   * Maps a map of categorized values with String keys to a map of values with {@link CategoryRef}
   * as key. Categories are defined in a particular risk definition so this method needs the risk
   * definition ID and a reference provider to find and create the {@link CategoryRef} instances. It
   * ensures that categories exist for the provided category IDs.
   */
  private <T> Map<CategoryRef, T> mapToCategories(
      Map<String, T> values,
      String riskDefinitionId,
      DomainRiskReferenceProvider referenceProvider) {
    return values.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .collect(
            Collectors.toMap(
                e -> toCategoryRef(e.getKey(), riskDefinitionId, referenceProvider),
                Entry::getValue));
  }

  private CategoryRef toCategoryRef(
      String categoryKey, String riskDefinitionId, DomainRiskReferenceProvider referenceProvider) {
    return referenceProvider.getCategoryRef(riskDefinitionId, categoryKey);
  }

  private RiskDefinitionRef toRiskDefinitionRef(String riskDefId, Domain domain) {
    if (riskDefId == null) {
      return null;
    }
    return referencesForDomain(domain).getRiskDefinitionRef(riskDefId);
  }
}
