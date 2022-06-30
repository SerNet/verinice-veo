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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractCatalogItemDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractVersionedSelfReferencingDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.VersionedDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.dto.full.FullCatalogDto;
import org.veo.adapter.presenter.api.dto.full.FullCatalogItemDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformLinkTailoringReference;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Incident;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;
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

  public VersionedDto transform2Dto(Versioned source) {
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
    if (source instanceof Catalog catalog) {
      return transformCatalog2Dto(catalog);
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
    FullAssetDto target = new FullAssetDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    return target;
  }

  public FullProcessDto transformProcess2Dto(@Valid Process source) {
    return transformProcess2Dto(source, false);
  }

  public FullProcessDto transformProcess2Dto(@Valid Process source, boolean embedRisks) {
    FullProcessDto target = new FullProcessDto();
    mapCompositeEntity(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);

    if (embedRisks) {
      target.setRisks(
          source.getRisks().stream()
              .map(this::transform2Dto)
              .map(ProcessRiskDto.class::cast)
              .collect(Collectors.toSet()));
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
    FullScopeDto target = new FullScopeDto();
    mapElement(source, target);
    domainAssociationTransformer.mapDomainsToDto(source, target);
    target.setMembers(convertReferenceSet(source.getMembers()));
    return target;
  }

  public FullDomainDto transformDomain2Dto(@Valid Domain source) {
    var target = new FullDomainDto();
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setAuthority(source.getAuthority());
    target.setRevision(source.getRevision());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setDecisions(source.getDecisions());

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setCatalogs(convertReferenceSet(source.getCatalogs()));
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));
    return target;
  }

  public TransformDomainTemplateDto transformDomainTemplate2Dto(@Valid DomainTemplate source) {
    var target = new TransformDomainTemplateDto();
    mapDomainTemplate(source, target);
    return target;
  }

  public TransformDomainDto transformDomain2ExportDto(@Valid Domain source) {
    var target = new TransformDomainDto();
    mapDomainTemplate(source, target);
    target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));
    return target;
  }

  private void mapDomainTemplate(DomainTemplate source, TransformDomainTemplateDto target) {
    target.setId(source.getId().uuidValue());
    target.setVersion(source.getVersion());
    target.setAuthority(source.getAuthority());
    target.setRevision(source.getRevision());
    target.setTemplateVersion(source.getTemplateVersion());

    mapVersionedSelfReferencingProperties(source, target);
    mapNameableProperties(source, target);
    target.setCatalogs(convertSet(source.getCatalogs(), this::transformCatalog2CompositeDto));

    Map<String, ElementTypeDefinitionDto> elementTypeDefinitionsByType =
        source.getElementTypeDefinitions().stream()
            .collect(
                Collectors.toMap(
                    ElementTypeDefinition::getElementType, this::mapElementTypeDefinition));

    target.setElementTypeDefinitions(elementTypeDefinitionsByType);
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

  public TransformCatalogDto transformCatalog2CompositeDto(@Valid Catalog source) {
    var target = new TransformCatalogDto();
    target.setId(source.getId().uuidValue());
    mapNameableProperties(source, target);
    mapVersionedSelfReferencingProperties(source, target);

    if (source.getDomainTemplate() != null) {
      target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));
    }
    target.setCatalogItems(convertSet(source.getCatalogItems(), this::transformCatalogItem2Dto));

    return target;
  }

  public TransformCatalogItemDto transformCatalogItem2Dto(@Valid CatalogItem source) {
    var target = new TransformCatalogItemDto();
    target.setId(source.getId().uuidValue());
    mapCatalogItem(source, target);
    Element element = source.getElement();
    if (element != null) {
      target.setElement(transform2Dto(element));
      DomainTemplate domainTemplate = source.getCatalog().getDomainTemplate();

      target.getElement().associateWithTargetDomain(domainTemplate.getIdAsString());
    }
    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformTailoringReference2Dto)
            .collect(Collectors.toSet()));

    return target;
  }

  public FullCatalogItemDto transformCatalogItem2Dto(
      @Valid CatalogItem source, boolean includeDescriptionFromElement) {
    FullCatalogItemDto target = new FullCatalogItemDto();
    target.setId(source.getId().uuidValue());
    mapCatalogItem(source, target);
    Element element = source.getElement();
    if (element != null) {
      target.setElement(IdRef.from(element, referenceAssembler));
      if (includeDescriptionFromElement) {
        target.setDescription(element.getDescription());
      }
    }
    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(this::transformTailoringReference2Dto)
            .collect(Collectors.toSet()));
    return target;
  }

  private void mapCatalogItem(CatalogItem source, AbstractCatalogItemDto target) {
    mapVersionedSelfReferencingProperties(source, target);
    target.setNamespace(source.getNamespace());
    if (source.getCatalog() != null) {
      target.setCatalog(IdRef.from(source.getCatalog(), referenceAssembler));
    }
  }

  public FullCatalogDto transformCatalog2Dto(@Valid Catalog source) {
    FullCatalogDto target = new FullCatalogDto();

    target.setId(source.getId().uuidValue());
    mapNameableProperties(source, target);
    mapVersionedSelfReferencingProperties(source, target);

    if (source.getDomainTemplate() != null) {
      target.setDomainTemplate(IdRef.from(source.getDomainTemplate(), referenceAssembler));
    }
    target.setCatalogItems(convertReferenceSet(source.getCatalogItems()));

    return target;
  }

  public AbstractTailoringReferenceDto transformTailoringReference2Dto(
      @Valid TailoringReference source) {
    AbstractTailoringReferenceDto target = null;
    if (source.isLinkTailoringReferences()) {
      LinkTailoringReference linkRef = (LinkTailoringReference) source;
      target =
          new TransformLinkTailoringReference(
              linkRef.getLinkType(), Map.copyOf(linkRef.getAttributes()));
    } else {
      target = new FullTailoringReferenceDto(source.getId().uuidValue());
    }

    target.setReferenceType(source.getReferenceType());

    if (source.getCatalogItem() != null) {
      target.setCatalogItem(IdRef.from(source.getCatalogItem(), referenceAssembler));
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
    if (source.getClient() != null) {
      target.setClient(IdRef.from(source.getClient(), referenceAssembler));
    }
    if (source.getParent() != null) {
      target.setParent(IdRef.from(source.getParent(), referenceAssembler));
    }

    return target;
  }

  public CustomLinkDto transformCustomLink2Dto(@Valid CustomLink source) {
    var target = new CustomLinkDto();
    target.setAttributes(source.getAttributes());

    target.setDomains(convertReferenceSet(source.getDomains()));
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
    target.setDomains(convertReferenceSet(source.getDomains()));
    return target;
  }

  private static void mapNameableProperties(Nameable source, NameableDto target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }

  private static void mapVersionedProperties(Versioned source, VersionedDto target) {
    target.setCreatedAt(source.getCreatedAt().toString());
    target.setCreatedBy(source.getCreatedBy());
    target.setUpdatedAt(source.getUpdatedAt().toString());
    target.setUpdatedBy(source.getUpdatedBy());
  }

  private static <TIn, TOut> Set<TOut> convertSet(Set<TIn> input, Function<TIn, TOut> mapper) {
    return input.stream().map(mapper).collect(Collectors.toSet());
  }

  private <T extends Identifiable> Set<IdRef<T>> convertReferenceSet(Set<T> domains) {
    return domains.stream().map(o -> IdRef.from(o, referenceAssembler)).collect(Collectors.toSet());
  }

  private Map<String, List<CustomLinkDto>> mapLinks(Set<CustomLink> links) {
    return links.stream().collect(groupingBy(CustomLink::getType)).entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(this::transformCustomLink2Dto).toList()));
  }

  private Map<String, CustomAspectDto> mapCustomAspects(Set<CustomAspect> customAspects) {
    return customAspects.stream()
        .collect(toMap(CustomAspect::getType, this::transformCustomAspect2Dto));
  }
}
