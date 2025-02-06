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
package org.veo.core.usecase.service;

import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.ProfileState;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.ControlImplementationTailoringReferenceState;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.DomainBaseState;
import org.veo.core.entity.state.ElementTypeDefinitionState;
import org.veo.core.entity.state.LinkTailoringReferenceState;
import org.veo.core.entity.state.ProfileItemState;
import org.veo.core.entity.state.RequirementImplementationTailoringReferenceState;
import org.veo.core.entity.state.RiskTailoringReferenceState;
import org.veo.core.entity.state.TailoringReferenceState;
import org.veo.core.entity.state.TemplateItemState;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.service.DomainTemplateIdGenerator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainStateMapper {
  private final RefResolverFactory refResolverFactory;
  private final EntityFactory entityFactory;
  private final DomainTemplateIdGenerator domainTemplateIdGenerator;

  public Domain toDomain(DomainBaseState source, boolean copyProfiles) {
    var target =
        entityFactory.createDomain(
            source.getName(), source.getAuthority(), source.getTemplateVersion());
    map(source, target, copyProfiles);
    return target;
  }

  public DomainTemplate toTemplate(DomainBaseState source) {
    var target =
        entityFactory.createDomainTemplate(
            source.getName(),
            source.getAuthority(),
            source.getTemplateVersion(),
            UUID.fromString(
                domainTemplateIdGenerator.createDomainTemplateId(
                    source.getName(), source.getTemplateVersion())));
    map(source, target, true);
    return target;
  }

  public ElementTypeDefinition toElementTypeDefinition(
      ElementType elementType, ElementTypeDefinitionState source, DomainBase owner) {
    var target = entityFactory.createElementTypeDefinition(elementType, owner);
    target.setSubTypes(source.getSubTypes());
    target.setCustomAspects(source.getCustomAspects());
    target.setLinks(source.getLinks());
    target.setTranslations(source.getTranslations());
    return target;
  }

  private void map(DomainBaseState source, DomainBase target, boolean copyProfiles) {
    var resolver = refResolverFactory.local();
    var domainRef = TypedId.from(source.getId(), source.getModelInterface());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
    target.setElementTypeDefinitions(
        source.getElementTypeDefinitionStates().entrySet().stream()
            .map(
                stringElementTypeDefinitionStateEntry ->
                    toElementTypeDefinition(
                        stringElementTypeDefinitionStateEntry.getKey(),
                        stringElementTypeDefinitionStateEntry.getValue(),
                        target))
            .collect(Collectors.toSet()));
    target.setRiskDefinitions(source.getRiskDefinitions());
    target.setDecisions(source.getDecisions());
    target.setInspections(source.getInspections());
    target.setIncarnationConfiguration(source.getIncarnationConfiguration());
    // TODO #3860 revert to direct mapping
    target.setControlImplementationConfiguration(
        source
            .getControlImplementationConfigurationDto()
            .toConfig(target.getControlImplementationConfiguration()));
    target.setDomainMigrationDefinition(source.getDomainMigrationDefinition());

    // Create all catalog items and register them in the resolver before mapping them, because they
    // may reference each other.
    target.setCatalogItems(
        source.getCatalogItemStates().stream()
            .map(
                ci ->
                    resolver.injectNewEntity(
                        TypedSymbolicId.from(ci.getSymbolicId(), CatalogItem.class, domainRef)))
            .collect(toSet()));
    source
        .getCatalogItemStates()
        .forEach(
            ciState ->
                mapTemplateItem(
                    ciState,
                    resolver.resolve(
                        TypedSymbolicId.from(
                            ciState.getSymbolicId(), CatalogItem.class, domainRef)),
                    resolver));
    if (copyProfiles) {
      target.setProfiles(
          source.getProfileStates().stream()
              .map(profileState -> toProfile(profileState, resolver, target))
              .collect(Collectors.toSet()));
    }
  }

  public Profile toProfile(ProfileState source, DomainBase owner) {
    var resolver = refResolverFactory.local();
    source.getItemStates().stream()
        .map(ProfileItemState::getAppliedCatalogItemRef)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(
            ciRef ->
                resolver.inject(
                    owner
                        .findCatalogItem(ciRef.getSymbolicId())
                        .orElseThrow(
                            () -> new UnprocessableDataException("%s not found".formatted(ciRef))),
                    ciRef));
    return toProfile(source, resolver, owner);
  }

  private Profile toProfile(ProfileState source, LocalRefResolver resolver, DomainBase owner) {
    var profileRef = TypedId.from(source.getId(), Profile.class);
    var target = entityFactory.createProfile(owner);
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setLanguage(source.getLanguage());
    target.setProductId(source.getProductId());

    // Create all profile items and register them in the resolver before mapping them, because they
    // may reference each other.
    target.setItems(
        source.getItemStates().stream()
            .map(
                itemState ->
                    resolver.injectNewEntity(
                        TypedSymbolicId.from(
                            itemState.getSymbolicId(), ProfileItem.class, profileRef)))
            .collect(toSet()));
    source
        .getItemStates()
        .forEach(
            itemState ->
                mapProfileItem(
                    itemState,
                    resolver.resolve(
                        TypedSymbolicId.from(
                            itemState.getSymbolicId(), ProfileItem.class, profileRef)),
                    resolver));

    return target;
  }

  private void mapProfileItem(ProfileItemState source, ProfileItem target, IdRefResolver resolver) {
    mapTemplateItem(source, target, resolver);
    target.setAppliedCatalogItem(
        Optional.ofNullable(source.getAppliedCatalogItemRef()).map(resolver::resolve).orElse(null));
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void mapTemplateItem(
          TemplateItemState<T, TNamespace> source, T target, IdRefResolver resolver) {
    EntityStateMapper.mapNameableProperties(source, target);
    target.setSymbolicId(source.getSymbolicId());
    target.setElementType(source.getElementType());
    target.setStatus(source.getStatus());
    target.setSubType(source.getSubType());
    target.setAspects(source.getAspects());
    target.setCustomAspects(
        source.getCustomAspectStates().stream()
            .collect(
                Collectors.toMap(CustomAspectState::getType, CustomAspectState::getAttributes)));
    source.getTailoringReferenceStates().forEach(tr -> addTailoringReference(tr, target, resolver));
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void addTailoringReference(
          TailoringReferenceState<T, TNamespace> source, T owner, IdRefResolver resolver) {
    var targetItem = resolver.resolve(source.getTargetRef());
    switch (source) {
      case LinkTailoringReferenceState<T, TNamespace> linkDto ->
          owner.addLinkTailoringReference(
              source.getReferenceType(),
              targetItem,
              linkDto.getLinkType(),
              linkDto.getAttributes());
      case RiskTailoringReferenceState<T, TNamespace> riskDto ->
          owner.addRiskTailoringReference(
              source.getReferenceType(),
              targetItem,
              Optional.ofNullable(riskDto.getRiskOwnerRef()).map(resolver::resolve).orElse(null),
              Optional.ofNullable(riskDto.getMitigationRef()).map(resolver::resolve).orElse(null),
              riskDto.getRiskDefinitions());
      case ControlImplementationTailoringReferenceState<T, TNamespace> ciDto ->
          owner.addControlImplementationReference(
              targetItem,
              Optional.ofNullable(ciDto.getResponsibleRef()).map(resolver::resolve).orElse(null),
              ciDto.getDescription());
      case RequirementImplementationTailoringReferenceState<T, TNamespace> riDto ->
          owner.addRequirementImplementationReference(
              targetItem,
              riDto.getStatus(),
              riDto.getImplementationStatement(),
              riDto.getImplementationUntil(),
              Optional.ofNullable(riDto.getResponsibleRef()).map(resolver::resolve).orElse(null));
      default -> owner.addTailoringReference(source.getReferenceType(), targetItem);
    }
  }
}
