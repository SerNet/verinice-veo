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
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.common.RequirementImplementationRef;
import org.veo.adapter.presenter.api.common.RequirementImplementationsRef;
import org.veo.adapter.presenter.api.dto.AbstractCatalogItemDto;
import org.veo.adapter.presenter.api.dto.AbstractCompositeElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractProfileTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractVersionedDto;
import org.veo.adapter.presenter.api.dto.AbstractVersionedSelfReferencingDto;
import org.veo.adapter.presenter.api.dto.AttributesDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.ControlImplementationDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomAspectMapDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.DomainTemplateMetadataDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.LinkDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.RiskAffectedDto;
import org.veo.adapter.presenter.api.dto.ShortCatalogItemDto;
import org.veo.adapter.presenter.api.dto.ShortProfileDto;
import org.veo.adapter.presenter.api.dto.ShortProfileItemDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullCatalogDto;
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
import org.veo.adapter.presenter.api.dto.full.FullProfileTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.dto.full.LegacyCatalogItemDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.ExportCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.ExportLinkProfileTailoringReference;
import org.veo.adapter.service.domaintemplate.dto.ExportLinkTailoringReference;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileItemDto;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomAttributeContainer;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.LinkTailoringReference;
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
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.definitions.ElementTypeDefinition;

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

  public AbstractVersionedDto transform2Dto(Versioned source) {
    if (source instanceof Element element) {
      return transform2Dto(element);
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
      return transformCatalogItem2Dto(catalogItem, false);
    }
    if (source instanceof TailoringReference tailoringReference) {
      return transformTailoringReference2Dto(tailoringReference);
    }
    throw new IllegalArgumentException(
        "No transform method defined for " + source.getClass().getSimpleName());
  }

  private ExportProfileDto transformProfile2ExportDto(Profile profile) {
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
    target.setId(source.getId().uuidValue());
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setElementType(source.getElementType());
    target.setSubType(source.getSubType());
    target.setStatus(source.getStatus());

    target.setCustomAspects(
        new CustomAspectMapDto(
            source.getCustomAspects().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> new AttributesDto(e.getValue())))));

    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformProfileTailoringReference2Dto)
            .collect(toSet()));
    // TODO #2301 remove
    target.setNamespace(source.getNamespace());

    return target;
  }

  private ExportProfileItemDto transformProfileItem2ExportDto(ProfileItem source) {
    ExportProfileItemDto target = new ExportProfileItemDto();
    target.setId(source.getId().uuidValue());
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setElementType(source.getElementType());
    target.setSubType(source.getSubType());
    target.setStatus(source.getStatus());

    target.setCustomAspects(
        new CustomAspectMapDto(
            source.getCustomAspects().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> new AttributesDto(e.getValue())))));

    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformProfileTailoringReference2Dto)
            .collect(toSet()));
    // TODO #2301 remove
    target.setNamespace(source.getNamespace());

    return target;
  }

  public AbstractProfileTailoringReferenceDto transformProfileTailoringReference2Dto(
      @Valid TailoringReference<ProfileItem> source) {
    AbstractProfileTailoringReferenceDto target;
    if (source instanceof LinkTailoringReference<ProfileItem> linkRef) {
      target =
          new ExportLinkProfileTailoringReference(
              linkRef.getLinkType(), Map.copyOf(linkRef.getAttributes()));
    } else {
      target = new FullProfileTailoringReferenceDto(source.getId().uuidValue());
    }
    // TODO: handle risk ref
    target.setReferenceType(source.getReferenceType());

    if (source.getTarget() != null) {
      target.setTarget(IdRef.from(source.getTarget(), referenceAssembler));
    }
    return target;
  }

  public AbstractElementDto transform2Dto(@Valid Element source) {

    if (source instanceof Person person) {
      return transformPerson2Dto(person);
    }
    if (source instanceof Asset asset) {
      return transformAsset2Dto(asset);
    }
    if (source instanceof Process process) {
      return transformProcess2Dto(process);
    }
    if (source instanceof Document document) {
      return transformDocument2Dto(document);
    }
    if (source instanceof Control control) {
      return transformControl2Dto(control);
    }
    if (source instanceof Incident incident) {
      return transformIncident2Dto(incident);
    }
    if (source instanceof Scenario scenario) {
      return transformScenario2Dto(scenario);
    }
    if (source instanceof Scope scope) {
      return transformScope2Dto(scope);
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

  public FullPersonDto transformPerson2Dto(@Valid Person source) {
    FullPersonDto target = new FullPersonDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullAssetDto transformAsset2Dto(@Valid Asset source) {
    return transformAsset2Dto(source, false);
  }

  public FullAssetDto transformAsset2Dto(@Valid Asset source, boolean embedRisks) {
    FullAssetDto target = new FullAssetDto();
    mapCompositeEntity(source, target);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);

    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(AssetRiskDto.class::cast)
              .collect(toSet()));
    }

    return target;
  }

  public FullProcessDto transformProcess2Dto(@Valid Process source) {
    return transformProcess2Dto(source, false);
  }

  public FullProcessDto transformProcess2Dto(@Valid Process source, boolean embedRisks) {
    FullProcessDto target = new FullProcessDto();
    mapCompositeEntity(source, target);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);

    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(ProcessRiskDto.class::cast)
              .collect(toSet()));
    }

    return target;
  }

  public FullDocumentDto transformDocument2Dto(@Valid Document source) {
    FullDocumentDto target = new FullDocumentDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullControlDto transformControl2Dto(@Valid Control source) {
    FullControlDto target = new FullControlDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullIncidentDto transformIncident2Dto(@Valid Incident source) {
    FullIncidentDto target = new FullIncidentDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullScenarioDto transformScenario2Dto(@Valid Scenario source) {
    FullScenarioDto target = new FullScenarioDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullScopeDto transformScope2Dto(@Valid Scope source) {
    return transformScope2Dto(source, false);
  }

  public FullScopeDto transformScope2Dto(@Valid Scope source, boolean embedRisks) {
    FullScopeDto target = new FullScopeDto();
    mapElement(source, target);
    mapRiskAffected(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
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
    target.setJsonProfiles(Map.copyOf(source.getJsonProfiles()));
    target.setElementTypeDefinitions(
        source.getElementTypeDefinitions().stream()
            .collect(toMap(ElementTypeDefinition::getElementType, this::mapElementTypeDefinition)));

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));

    target.setProfilesNew(convertSet(source.getProfiles(), this::transformProfile2Dto));

    return target;
  }

  public ExportDomainTemplateDto transformDomainTemplate2Dto(@Valid DomainBase source) {
    var target = new ExportDomainTemplateDto();
    mapDomain(source, target);

    target.setProfilesNew(convertSet(source.getProfiles(), this::transformProfile2ExportDto));

    return target;
  }

  public ExportDomainDto transformDomain2ExportDto(@Valid Domain source) {
    var target = new ExportDomainDto();
    mapDomain(source, target);
    target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));

    target.setProfilesNew(convertSet(source.getProfiles(), this::transformProfile2ExportDto));

    return target;
  }

  private void mapDomain(DomainBase source, ExportDomainTemplateDto target) {
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setAuthority(source.getAuthority());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setJsonProfiles(Map.copyOf(source.getJsonProfiles()));

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setCatalogItems(convertSet(source.getCatalogItems(), this::transformCatalogItem2Dto));
    Map<String, ElementTypeDefinitionDto> elementTypeDefinitionsByType =
        source.getElementTypeDefinitions().stream()
            .collect(toMap(ElementTypeDefinition::getElementType, this::mapElementTypeDefinition));

    target.setElementTypeDefinitions(elementTypeDefinitionsByType);
    target.setDecisions(Map.copyOf(source.getDecisions()));
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
    target.setId(source.getId().uuidValue());
    mapCatalogItem(source, target);
    target.setStatus(source.getStatus());

    target.setCustomAspects(
        new CustomAspectMapDto(
            source.getCustomAspects().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> new AttributesDto(e.getValue())))));

    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformTailoringReference2Dto)
            .collect(toSet()));
    // TODO #2301 remove
    target.setNamespace(source.getNamespace());
    return target;
  }

  public ShortCatalogItemDto transformShortCatalogItem2Dto(@Valid CatalogItem source) {
    var target = new ShortCatalogItemDto();
    target.setId(source.getId().uuidValue());
    mapCatalogItem(source, target);
    mapVersionedSelfReferencingProperties(source, target);
    return target;
  }

  public ShortProfileItemDto transformShortProfileItem2Dto(@Valid ProfileItem source) {
    var target = new ShortProfileItemDto();
    target.setId(source.getId().uuidValue());
    mapCatalogItem(source, target);
    target.setStatus(source.getStatus());
    mapVersionedSelfReferencingProperties(source, target);
    return target;
  }

  private void mapCatalogItem(TemplateItem source, AbstractCatalogItemDto target) {
    mapNameableProperties(source, target);
    target.setElementType(source.getElementType());
    target.setSubType(source.getSubType());
  }

  @Deprecated // TODO #2301 remove
  public LegacyCatalogItemDto transformCatalogItem2Dto(
      @Valid CatalogItem source, boolean includeDescriptionFromElement) {
    LegacyCatalogItemDto target = new LegacyCatalogItemDto();
    target.setId(source.getId().uuidValue());
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setNamespace(source.getNamespace());
    if (includeDescriptionFromElement) {
      target.setDescription(source.getDescription());
    }
    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformTailoringReference2Dto)
            .collect(toSet()));
    return target;
  }

  @Deprecated() // TODO #2301 remove
  public FullCatalogDto transformCatalog2Dto(@Valid Domain source) {
    FullCatalogDto target = new FullCatalogDto();

    target.setId(source.getId().uuidValue());
    target.setDomainTemplate(IdRef.from(source, referenceAssembler));
    mapNameableProperties(source, target);
    mapVersionedSelfReferencingProperties(source, target);

    if (source.getDomainTemplate() != null) {
      target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));
    }
    target.setCatalogItems(convertReferenceSet(source.getCatalogItems()));

    return target;
  }

  public AbstractTailoringReferenceDto transformTailoringReference2Dto(
      @Valid TailoringReference<CatalogItem> source) {
    AbstractTailoringReferenceDto target = null;
    if (source.isLinkTailoringReferences()) {
      LinkTailoringReference linkRef = (LinkTailoringReference) source;
      target =
          new ExportLinkTailoringReference(
              linkRef.getLinkType(), Map.copyOf(linkRef.getAttributes()));
    } else {
      target = new FullTailoringReferenceDto(source.getId().uuidValue());
    }

    target.setReferenceType(source.getReferenceType());

    if (source.getTarget() != null) {
      target.setCatalogItem(IdRef.from(source.getTarget(), referenceAssembler));
    }
    return target;
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
      Element source, TDto target) {
    target.setId(source.getId().uuidValue());
    target.setDesignator(source.getDesignator());
    target.setVersion(source.getVersion());
    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);

    target.setLinks(mapLinks(source.getLinks()));
    target.setCustomAspects(mapCustomAspects(source.getCustomAspects()));
    target.setType(source.getModelType());

    if (source.getOwner() != null) {
      target.setOwner(IdRef.from(source.getOwner(), referenceAssembler));
    }
  }

  private <T extends RiskAffected<T, ?>> void mapRiskAffected(T source, RiskAffectedDto<T> target) {
    target.setControlImplementations(
        source.getControlImplementations().stream()
            .map(ci -> mapControlImplementation(source, ci))
            .collect(toSet()));
  }

  private ControlImplementationDto mapControlImplementation(
      RiskAffected<?, ?> riskAffected, ControlImplementation source) {
    return new ControlImplementationDto(
        IdRef.from(source.getControl(), referenceAssembler),
        riskAffected.getRequirementImplementations().stream()
            .filter(ri -> ri.getControl().equals(source.getControl()))
            .findAny()
            .get()
            .getStatus(),
        source.getDescription(),
        IdRef.from(source.getResponsible(), referenceAssembler),
        RequirementImplementationsRef.from(source, referenceAssembler));
  }

  private <TDto extends Identifiable & Versioned> void mapVersionedSelfReferencingProperties(
      TDto source, AbstractVersionedSelfReferencingDto target) {
    target.setSelfRef(IdRef.from(source, referenceAssembler));
    mapVersionedProperties(source, target);
  }

  private <
          TEntity extends CompositeElement,
          TDto extends CompositeEntityDto<TEntity> & IdentifiableDto>
      void mapCompositeEntity(CompositeElement<TEntity> source, TDto target) {
    mapElement(source, target);
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
    target.setMembers(
        source.getMembers().stream()
            .map(m -> ElementInDomainIdRef.from(m, domain, referenceAssembler))
            .collect(toSet()));
    target.setRiskDefinition(domainAssociationTransformer.mapRiskDefinition(source, domain));
    target.setRiskValues(domainAssociationTransformer.mapRiskValues(source, domain));
    return target;
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
    target.setCustomAspects(mapCustomAspects(source, domain));
    target.setLinks(mapLinks(source, domain));
    target.setOwner(IdRef.from(source.getOwner(), referenceAssembler));
    target.setDecisionResults(source.getDecisionResults(domain));
  }

  private LinkMapDto mapLinks(Element source, Domain domain) {
    return new LinkMapDto(
        source.getLinks(domain).stream()
            .collect(groupingBy(CustomLink::getType))
            .entrySet()
            .stream()
            .collect(
                toMap(
                    Map.Entry::getKey,
                    kv -> kv.getValue().stream().map(l -> mapLink(l, domain)).toList())));
  }

  private CustomAspectMapDto mapCustomAspects(Element source, Domain domain) {
    return new CustomAspectMapDto(
        source.getCustomAspects(domain).stream()
            .collect(toMap(CustomAspect::getType, this::mapCustomAttributeContainer)));
  }

  private AttributesDto mapCustomAttributeContainer(CustomAttributeContainer source) {
    return new AttributesDto(source.getAttributes());
  }

  private LinkDto mapLink(CustomLink source, Domain domain) {
    return new LinkDto(
        ElementInDomainIdRef.from(source.getTarget(), domain, referenceAssembler),
        mapCustomAttributeContainer(source));
  }
}
