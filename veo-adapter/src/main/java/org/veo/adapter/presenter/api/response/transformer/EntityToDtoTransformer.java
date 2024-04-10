/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.adapter.presenter.api.response.transformer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.validation.Valid;

import javax.annotation.Nullable;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.RequirementImplementationRef;
import org.veo.adapter.presenter.api.common.RequirementImplementationsRef;
import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.adapter.presenter.api.dto.AbstractCompositeElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.AbstractTemplateItemDto;
import org.veo.adapter.presenter.api.dto.AbstractVersionedDto;
import org.veo.adapter.presenter.api.dto.AbstractVersionedSelfReferencingDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.ControlImplementationDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.DomainTemplateMetadataDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.RiskAffectedDto;
import org.veo.adapter.presenter.api.dto.RiskAffectedDtoWithRIs;
import org.veo.adapter.presenter.api.dto.ShortCatalogItemDto;
import org.veo.adapter.presenter.api.dto.ShortInspectionDto;
import org.veo.adapter.presenter.api.dto.ShortProfileDto;
import org.veo.adapter.presenter.api.dto.ShortProfileItemDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullControlInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProfileDto;
import org.veo.adapter.presenter.api.dto.full.FullProfileItemDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.ExportCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileItemDto;
import org.veo.adapter.service.domaintemplate.dto.FullTemplateItemDto;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.inspection.Inspection;

/** A collection of transform functions to transform entities to Dto back and forth. */
public final class EntityToDtoTransformer {

  private final ReferenceAssembler referenceAssembler;
  private final DomainAssociationTransformer domainAssociationTransformer;

  public EntityToDtoTransformer(
      ReferenceAssembler referenceAssembler,
      DomainAssociationTransformer domainAssociationTransformer) {
    this.referenceAssembler = referenceAssembler;
    this.domainAssociationTransformer = domainAssociationTransformer;
  }

  public AbstractVersionedDto transform2Dto(Versioned source, boolean newStructure) {
    if (source instanceof Element element) {
      return transform2Dto(element, newStructure);
    }
    if (source instanceof Domain domain) {
      return transformDomain2Dto(domain);
    }
    if (source instanceof Unit unit) {
      return transformUnit2Dto(unit);
    }
    if (source instanceof AbstractRisk abstractRisk) {
      return transform2Dto(abstractRisk);
    }
    if (source instanceof Profile profile) {
      return transformProfile2Dto(profile);
    }
    if (source instanceof ProfileItem profileitem) {
      return transformProfileItem2Dto(profileitem);
    }
    if (source instanceof CatalogItem catalogItem) {
      return transformCatalogItem2Dto(catalogItem);
    }
    throw new IllegalArgumentException(
        "No transform method defined for " + source.getClass().getSimpleName());
  }

  public ExportProfileDto transformProfile2ExportDto(Profile profile) {
    ExportProfileDto profileDto = new ExportProfileDto();
    profileDto.setId(profile.getIdAsString());
    profileDto.setName(profile.getName());
    profileDto.setDescription(profile.getDescription());
    profileDto.setLanguage(profile.getLanguage());
    profileDto.setItems(convertSet(profile.getItems(), this::transformProfileItem2ExportDto));
    return profileDto;
  }

  public FullProfileDto transformProfile2Dto(Profile profile) {
    FullProfileDto profileDto = new FullProfileDto();
    profileDto.setId(profile.getIdAsString());
    profileDto.setName(profile.getName());
    profileDto.setDescription(profile.getDescription());
    profileDto.setLanguage(profile.getLanguage());
    profileDto.setItems(convertSet(profile.getItems(), this::transformProfileItem2Dto));
    return profileDto;
  }

  public ShortProfileDto transformProfile2ListDto(Profile profile) {
    ShortProfileDto profileDto = new ShortProfileDto();
    profileDto.setId(profile.getIdAsString());
    profileDto.setName(profile.getName());
    profileDto.setDescription(profile.getDescription());
    profileDto.setLanguage(profile.getLanguage());
    return profileDto;
  }

  public FullProfileItemDto transformProfileItem2Dto(ProfileItem source) {
    FullProfileItemDto target = new FullProfileItemDto();
    mapFullTemplateItem(source, target);
    return target;
  }

  private ExportProfileItemDto transformProfileItem2ExportDto(ProfileItem source) {
    var target = new ExportProfileItemDto();
    mapFullTemplateItem(source, target);
    Optional.ofNullable(source.getAppliedCatalogItem())
        .map(ci -> SymIdRef.from(ci, referenceAssembler))
        .ifPresent(target::setAppliedCatalogItem);
    return target;
  }

  public AbstractElementDto transform2Dto(@Valid Element source, boolean newStructure) {

    if (source instanceof Person person) {
      return transformPerson2Dto(person, newStructure);
    }
    if (source instanceof Asset asset) {
      return transformAsset2Dto(asset, newStructure);
    }
    if (source instanceof Process process) {
      return transformProcess2Dto(process, newStructure);
    }
    if (source instanceof Document document) {
      return transformDocument2Dto(document, newStructure);
    }
    if (source instanceof Control control) {
      return transformControl2Dto(control, newStructure);
    }
    if (source instanceof Incident incident) {
      return transformIncident2Dto(incident, newStructure);
    }
    if (source instanceof Scenario scenario) {
      return transformScenario2Dto(scenario, newStructure);
    }
    if (source instanceof Scope scope) {
      return transformScope2Dto(scope, newStructure);
    }
    throw new IllegalArgumentException(
        "No transform method defined for " + source.getClass().getSimpleName());
  }

  public AbstractRiskDto transform2Dto(@Valid AbstractRisk<?, ?> source) {
    if (source instanceof AssetRisk assetRisk) {
      return AssetRiskDto.from(assetRisk, referenceAssembler);
    }
    if (source instanceof ProcessRisk processRisk) {
      return ProcessRiskDto.from(processRisk, referenceAssembler);
    }
    if (source instanceof ScopeRisk scopeRisk) {
      return ScopeRiskDto.from(scopeRisk, referenceAssembler);
    }
    throw new IllegalArgumentException(
        "No transform method defined for risk type " + source.getClass().getSimpleName());
  }

  public FullPersonDto transformPerson2Dto(@Valid Person source, boolean newStructure) {
    FullPersonDto target = new FullPersonDto();
    mapCompositeEntity(source, target, newStructure);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    return target;
  }

  public FullAssetDto transformAsset2Dto(@Valid Asset source, boolean newStructure) {
    return transformAsset2Dto(source, newStructure, false);
  }

  public FullAssetDto transformAsset2Dto(
      @Valid Asset source, boolean newStructure, boolean embedRisks) {
    FullAssetDto target = new FullAssetDto();
    mapCompositeEntity(source, target, newStructure);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);

    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(AssetRiskDto.class::cast)
              .collect(toSet()));
    }

    return target;
  }

  public FullProcessDto transformProcess2Dto(@Valid Process source, boolean newStructure) {
    return transformProcess2Dto(source, newStructure, false);
  }

  public FullProcessDto transformProcess2Dto(
      @Valid Process source, boolean newStructure, boolean embedRisks) {
    FullProcessDto target = new FullProcessDto();
    mapCompositeEntity(source, target, newStructure);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);

    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(ProcessRiskDto.class::cast)
              .collect(toSet()));
    }

    return target;
  }

  public FullDocumentDto transformDocument2Dto(@Valid Document source, boolean newStructure) {
    FullDocumentDto target = new FullDocumentDto();
    mapCompositeEntity(source, target, newStructure);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    return target;
  }

  public FullControlDto transformControl2Dto(@Valid Control source, boolean newStructure) {
    FullControlDto target = new FullControlDto();
    mapCompositeEntity(source, target, newStructure);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    return target;
  }

  public FullIncidentDto transformIncident2Dto(@Valid Incident source, boolean newStructure) {
    FullIncidentDto target = new FullIncidentDto();
    mapCompositeEntity(source, target, newStructure);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    return target;
  }

  public FullScenarioDto transformScenario2Dto(@Valid Scenario source, boolean newStructure) {
    FullScenarioDto target = new FullScenarioDto();
    mapCompositeEntity(source, target, newStructure);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    return target;
  }

  public FullScopeDto transformScope2Dto(@Valid Scope source, boolean newStructure) {
    return transformScope2Dto(source, newStructure, false);
  }

  public FullScopeDto transformScope2Dto(
      @Valid Scope source, boolean newStructure, boolean embedRisks) {
    FullScopeDto target = new FullScopeDto();
    mapElement(source, target, newStructure);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target, newStructure);
    target.setMembers(convertReferenceSet(source.getMembers()));
    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(ScopeRiskDto.class::cast)
              .collect(toSet()));
    }

    return target;
  }

  public RequirementImplementationDto transformRequirementImplementation2Dto(
      RequirementImplementation source) {
    var target = new RequirementImplementationDto();
    target.setSelfRef(RequirementImplementationRef.from(source, referenceAssembler));
    target.setControl(IdRef.from(source.getControl(), referenceAssembler));
    target.setStatus(source.getStatus());
    target.setOrigin(IdRef.from(source.getOrigin(), referenceAssembler));
    Optional.ofNullable(source.getResponsible())
        .map(r -> IdRef.from(r, referenceAssembler))
        .ifPresent(target::setResponsible);
    target.setImplementationStatement(source.getImplementationStatement());
    target.setOrigination(source.getOrigination());
    return target;
  }

  public FullDomainDto transformDomain2Dto(@Valid Domain source) {
    var target = new FullDomainDto();
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setAuthority(source.getAuthority());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setDecisions(source.getDecisions());
    target.setElementTypeDefinitions(
        source.getElementTypeDefinitions().stream()
            .collect(toMap(ElementTypeDefinition::getElementType, this::mapElementTypeDefinition)));

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));

    return target;
  }

  public ExportDomainTemplateDto transformDomainTemplate2Dto(@Valid DomainBase source) {
    var target = new ExportDomainTemplateDto();
    mapDomain(source, target);
    return target;
  }

  public ExportDomainDto transformDomain2ExportDto(@Valid Domain source) {
    var target = new ExportDomainDto();
    mapDomain(source, target);
    target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));
    return target;
  }

  private void mapDomain(DomainBase source, ExportDomainTemplateDto target) {
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setAuthority(source.getAuthority());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setProfilesNew(convertSet(source.getProfiles(), this::transformProfile2ExportDto));

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setCatalogItems(convertSet(source.getCatalogItems(), this::transformCatalogItem2Dto));
    Map<String, ElementTypeDefinitionDto> elementTypeDefinitionsByType =
        source.getElementTypeDefinitions().stream()
            .collect(toMap(ElementTypeDefinition::getElementType, this::mapElementTypeDefinition));

    target.setElementTypeDefinitions(elementTypeDefinitionsByType);
    target.setDecisions(Map.copyOf(source.getDecisions()));
    target.setInspections(Map.copyOf(source.getInspections()));
    target.setIncarnationConfiguration(source.getIncarnationConfiguration());
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));
  }

  private ElementTypeDefinitionDto mapElementTypeDefinition(
      ElementTypeDefinition elementTypeDefinition) {
    ElementTypeDefinitionDto elementTypeDefinitionDto = new ElementTypeDefinitionDto();
    elementTypeDefinitionDto.setSubTypes(elementTypeDefinition.getSubTypes());
    elementTypeDefinitionDto.setCustomAspects(elementTypeDefinition.getCustomAspects());
    elementTypeDefinitionDto.setLinks(elementTypeDefinition.getLinks());
    elementTypeDefinitionDto.setTranslations(elementTypeDefinition.getTranslations());
    return elementTypeDefinitionDto;
  }

  public ExportCatalogItemDto transformCatalogItem2Dto(@Valid CatalogItem source) {
    var target = new ExportCatalogItemDto();
    mapFullTemplateItem(source, target);
    return target;
  }

  public ShortCatalogItemDto transformShortCatalogItem2Dto(@Valid CatalogItem source) {
    var target = new ShortCatalogItemDto();
    target.setId(source.getSymbolicIdAsString());
    mapTemplateItem(source, target);
    return target;
  }

  public ShortProfileItemDto transformShortProfileItem2Dto(@Valid ProfileItem source) {
    var target = new ShortProfileItemDto();
    target.setId(source.getSymbolicIdAsString());
    target.setStatus(source.getStatus());
    mapTemplateItem(source, target);
    return target;
  }

  private <
          TEntity extends TemplateItem<TEntity, TNamespace>,
          TNamespace extends Identifiable,
          TDto extends
              AbstractTemplateItemDto<TEntity, TNamespace>
                  & FullTemplateItemDto<TEntity, TNamespace>>
      void mapFullTemplateItem(TEntity source, TDto target) {
    mapVersionedSelfReferencingProperties(source, target);
    mapTemplateItem(source, target);
    target.setId(source.getSymbolicIdAsString());
    target.setStatus(source.getStatus());
    target.setAspects(source.getAspects());
    target.setCustomAspects(CustomAspectMapDto.from(source.getCustomAspects()));
    source
        .getTailoringReferences()
        .forEach(tailoringReference -> target.add(tailoringReference, referenceAssembler));
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void mapTemplateItem(T source, AbstractTemplateItemDto<T, TNamespace> target) {
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setElementType(source.getElementType());
    target.setSubType(source.getSubType());
  }

  public FullUnitDto transformUnit2Dto(@Valid Unit source) {
    var target = new FullUnitDto();
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setUnits(convertSet(source.getUnits(), u -> IdRef.from(u, referenceAssembler)));
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);

    target.setDomains(convertReferenceSet(source.getDomains()));
    if (source.getParent() != null) {
      target.setParent(IdRef.from(source.getParent(), referenceAssembler));
    }

    return target;
  }

  public CustomLinkDto transformCustomLink2Dto(@Valid CustomLink source) {
    var target = new CustomLinkDto();
    target.setAttributes(source.getAttributes());

    target.setDomains(convertReferenceSet(Set.of(source.getDomain())));
    if (source.getTarget() != null) {
      target.setTarget(IdRef.from(source.getTarget(), referenceAssembler));
    }

    return target;
  }

  private <TDto extends AbstractElementDto & IdentifiableDto> void mapElement(
      Element source, TDto target, boolean newStructure) {
    target.setId(source.getId().uuidValue());
    target.setDesignator(source.getDesignator());
    target.setVersion(source.getVersion());
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setLinks(newStructure ? null : mapLinks(source.getLinks()));
    target.setCustomAspects(newStructure ? null : mapCustomAspects(source.getCustomAspects()));
    target.setType(source.getModelType());

    if (source.getOwner() != null) {
      target.setOwner(IdRef.from(source.getOwner(), referenceAssembler));
    }
  }

  private <T extends RiskAffected<T, ?>> void mapRiskAffected(
      T source, RiskAffectedDtoWithRIs<T> target) {
    target.setRequirementImplementations(
        source.getRequirementImplementations().stream()
            .map(this::transformRequirementImplementation2Dto)
            .collect(toSet()));
    mapRiskAffectedProperties(source, target, null);
  }

  private ControlImplementationDto mapControlImplementation(
      RiskAffected<?, ?> riskAffected, ControlImplementation source, @Nullable Domain domain) {
    return new ControlImplementationDto(
        ref(source.getControl(), domain),
        riskAffected.getRequirementImplementations().stream()
            .filter(ri -> ri.getControl().equals(source.getControl()))
            .findAny()
            .get()
            .getStatus(),
        source.getDescription(),
        ref(source.getResponsible(), domain),
        RequirementImplementationsRef.from(source, referenceAssembler));
  }

  private <T extends Element> IdRef<T> ref(T element, @Nullable Domain domain) {
    return domain != null
        ? ElementInDomainIdRef.from(element, domain, referenceAssembler)
        : IdRef.from(element, referenceAssembler);
  }

  private <TEntity extends Identifiable & Versioned> void mapVersionedSelfReferencingProperties(
      TEntity source, AbstractVersionedSelfReferencingDto target) {
    target.setSelfRef(IdRef.from(source, referenceAssembler));
    mapVersionedProperties(source, target);
  }

  private <
          TEntity extends SymIdentifiable<TEntity, TNamespace> & Versioned,
          TNamespace extends Identifiable>
      void mapVersionedSelfReferencingProperties(
          TEntity source, AbstractVersionedSelfReferencingDto target) {
    target.setSelfRef(SymIdRef.from(source, referenceAssembler));
    mapVersionedProperties(source, target);
  }

  private <
          TEntity extends CompositeElement<TEntity>,
          TDto extends CompositeEntityDto<TEntity> & IdentifiableDto>
      void mapCompositeEntity(CompositeElement<TEntity> source, TDto target, boolean newStructure) {
    mapElement(source, target, newStructure);
    target.setParts(convertReferenceSet(source.getParts()));
  }

  public CustomAspectDto transformCustomAspect2Dto(@Valid CustomAspect source) {
    var target = new CustomAspectDto();
    target.setAttributes(source.getAttributes());
    target.setDomains(convertReferenceSet(Set.of(source.getDomain())));
    return target;
  }

  private static void mapNameableProperties(Nameable source, NameableDto target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }

  private static void mapVersionedProperties(Versioned source, AbstractVersionedDto target) {
    target.setCreatedAt(source.getCreatedAt().toString());
    target.setCreatedBy(source.getCreatedBy());
    target.setUpdatedAt(source.getUpdatedAt().toString());
    target.setUpdatedBy(source.getUpdatedBy());
  }

  private static <TIn, TOut> Set<TOut> convertSet(Set<TIn> input, Function<TIn, TOut> mapper) {
    return input.stream().map(mapper).collect(toSet());
  }

  private <T extends Identifiable> Set<IdRef<T>> convertReferenceSet(Set<T> domains) {
    return domains.stream().map(o -> IdRef.from(o, referenceAssembler)).collect(toSet());
  }

  private Map<String, List<CustomLinkDto>> mapLinks(Set<CustomLink> links) {
    return links.stream().collect(groupingBy(CustomLink::getType)).entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(this::transformCustomLink2Dto).toList()));
  }

  private Map<String, CustomAspectDto> mapCustomAspects(Set<CustomAspect> customAspects) {
    var map = new HashMap<String, CustomAspectDto>();
    customAspects.forEach(ca -> map.putIfAbsent(ca.getType(), transformCustomAspect2Dto(ca)));
    return map;
  }

  public DomainTemplateMetadataDto transformDomainTemplateMetadata2Dto(DomainTemplate source) {
    var target = new DomainTemplateMetadataDto();
    target.setId(source.getId().uuidValue());
    target.setSelfRef(IdRef.from(source, referenceAssembler));
    target.setName(source.getName());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setCreatedAt(source.getCreatedAt().toString());
    return target;
  }

  public <TElement extends Element> AbstractElementInDomainDto<TElement> transformElement2Dto(
      TElement element, Domain domain) {
    if (element instanceof Asset asset) {
      return (AbstractElementInDomainDto<TElement>) transformAsset2Dto(asset, domain);
    }
    if (element instanceof Control control) {
      return (AbstractElementInDomainDto<TElement>) transformControl2Dto(control, domain);
    }
    if (element instanceof Document document) {
      return (AbstractElementInDomainDto<TElement>) transformDocument2Dto(document, domain);
    }
    if (element instanceof Person person) {
      return (AbstractElementInDomainDto<TElement>) transformPerson2Dto(person, domain);
    }
    if (element instanceof Process process) {
      return (AbstractElementInDomainDto<TElement>) transformProcess2Dto(process, domain);
    }
    if (element instanceof Scenario scenario) {
      return (AbstractElementInDomainDto<TElement>) transformScenario2Dto(scenario, domain);
    }
    if (element instanceof Scope scope) {
      return (AbstractElementInDomainDto<TElement>) transformScope2Dto(scope, domain);
    }
    throw new IllegalArgumentException();
  }

  public FullAssetInDomainDto transformAsset2Dto(Asset source, Domain domain) {
    var target = new FullAssetInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    mapRiskAffectedProperties(source, target, domain);
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
  }

  public FullControlInDomainDto transformControl2Dto(Control source, Domain domain) {
    var target = new FullControlInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
  }

  public FullDocumentInDomainDto transformDocument2Dto(Document source, Domain domain) {
    var target = new FullDocumentInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    return target;
  }

  public FullIncidentInDomainDto transformIncident2Dto(Incident source, Domain domain) {
    var target = new FullIncidentInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    return target;
  }

  public FullPersonInDomainDto transformPerson2Dto(Person source, Domain domain) {
    var target = new FullPersonInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    return target;
  }

  public FullProcessInDomainDto transformProcess2Dto(Process source, Domain domain) {
    var target = new FullProcessInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    mapRiskAffectedProperties(source, target, domain);
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
  }

  public FullScenarioInDomainDto transformScenario2Dto(Scenario source, Domain domain) {
    var target = new FullScenarioInDomainDto(source.getIdAsString());
    mapCompositeElementProperties(source, target, domain);
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
  }

  public FullScopeInDomainDto transformScope2Dto(Scope source, Domain domain) {
    var target = new FullScopeInDomainDto(source.getIdAsString());
    mapElementProperties(source, target, domain);
    mapRiskAffectedProperties(source, target, domain);
    target.setMembers(
        source.getMembers().stream()
            .map(m -> ElementInDomainIdRef.from(m, domain, referenceAssembler))
            .collect(toSet()));
    target.setRiskDefinition(domainAssociationTransformer.mapRiskDefinition(source, domain));
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
  }

  private <TElement extends RiskAffected<TElement, ?>> void mapRiskAffectedProperties(
      TElement source, RiskAffectedDto<TElement> target, Domain domain) {
    target.setControlImplementations(
        source.getControlImplementations().stream()
            .map(ci -> mapControlImplementation(source, ci, domain))
            .collect(toSet()));
  }

  private <TElement extends CompositeElement<TElement>> void mapCompositeElementProperties(
      TElement source, AbstractCompositeElementInDomainDto<TElement> target, Domain domain) {
    mapElementProperties(source, target, domain);
    target.setParts(
        source.getParts().stream()
            .map(p -> ElementInDomainIdRef.from(p, domain, referenceAssembler))
            .collect(toSet()));
  }

  private <TElement extends Element> void mapElementProperties(
      TElement source, AbstractElementInDomainDto<TElement> target, Domain domain) {
    mapNameableProperties(source, target);
    mapVersionedProperties(source, target);
    target.setSelfRef(ElementInDomainIdRef.from(source, domain, referenceAssembler));
    target.setDesignator(source.getDesignator());
    target.setSubType(source.getSubType(domain));
    target.setStatus(source.getStatus(domain));
    target.setCustomAspects(CustomAspectMapDto.from(source, domain));
    target.setLinks(LinkMapDto.from(source, domain, referenceAssembler));
    target.setOwner(IdRef.from(source.getOwner(), referenceAssembler));
    target.setDecisionResults(source.getDecisionResults(domain));
  }

  public List<ShortInspectionDto> transformInspections2ShortDtos(
      Map<String, Inspection> inspections, Domain domain) {
    return inspections.entrySet().stream()
        .map(entry -> transformInspection2ShortDto(entry.getKey(), entry.getValue(), domain))
        .toList();
  }

  private ShortInspectionDto transformInspection2ShortDto(
      String id, Inspection source, Domain domain) {
    var target = new ShortInspectionDto();
    target.setId(id);
    target.setDescription(source.getDescription());
    target.setElementType(source.getElementType());
    target.setSeverity(source.getSeverity());
    target.setSelfRef(() -> referenceAssembler.inspectionReferenceOf(id, domain));
    return target;
  }
}
