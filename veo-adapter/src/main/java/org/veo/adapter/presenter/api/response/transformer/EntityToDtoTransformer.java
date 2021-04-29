/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.response.transformer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.VersionedDto;
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
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CompositeEntity;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class EntityToDtoTransformer {

    private final ReferenceAssembler referenceAssembler;
    private final SubTypeTransformer subTypeTransformer;

    public EntityToDtoTransformer(ReferenceAssembler referenceAssembler,
            SubTypeTransformer subTypeTransformer) {
        this.referenceAssembler = referenceAssembler;
        this.subTypeTransformer = subTypeTransformer;
    }

    public VersionedDto transform2Dto(Versioned source) {
        if (source instanceof EntityLayerSupertype) {
            return transform2Dto((EntityLayerSupertype) source);
        }
        if (source instanceof Domain) {
            return transformDomain2Dto((Domain) source);
        }
        if (source instanceof Unit) {
            return transformUnit2Dto((Unit) source);
        }
        if (source instanceof Catalog) {
            return transformCatalog2Dto((Catalog) source);
        }
        if (source instanceof CatalogItem) {
            return transformCatalogItem2Dto((CatalogItem) source);
        }
        if (source instanceof TailoringReference) {
            return transformTailoringReference2Dto((TailoringReference) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    public EntityLayerSupertypeDto transform2Dto(@Valid EntityLayerSupertype source) {

        if (source instanceof Person) {
            return transformPerson2Dto((Person) source);
        }
        if (source instanceof Asset) {
            return transformAsset2Dto((Asset) source);
        }
        if (source instanceof Process) {
            return transformProcess2Dto((Process) source);
        }
        if (source instanceof Document) {
            return transformDocument2Dto((Document) source);
        }
        if (source instanceof Control) {
            return transformControl2Dto((Control) source);
        }
        if (source instanceof Incident) {
            return transformIncident2Dto((Incident) source);
        }
        if (source instanceof Scenario) {
            return transformScenario2Dto((Scenario) source);
        }
        if (source instanceof Scope) {
            return transformScope2Dto((Scope) source);
        }
        throw new IllegalArgumentException("No transform method defined for " + source.getClass()
                                                                                      .getSimpleName());
    }

    // Person ->
    // PersonDto
    public FullPersonDto transformPerson2Dto(@Valid Person source) {
        FullPersonDto target = new FullPersonDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Asset -> AssetDto
    public FullAssetDto transformAsset2Dto(@Valid Asset source) {
        FullAssetDto target = new FullAssetDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Process ->
    // ProcessDto
    public FullProcessDto transformProcess2Dto(@Valid Process source) {
        FullProcessDto target = new FullProcessDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Document ->
    // DocumentDto
    public FullDocumentDto transformDocument2Dto(@Valid Document source) {
        FullDocumentDto target = new FullDocumentDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Control ->
    // ControlDto
    public FullControlDto transformControl2Dto(@Valid Control source) {
        FullControlDto target = new FullControlDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Incident ->
    // IncidentDto
    public FullIncidentDto transformIncident2Dto(@Valid Incident source) {
        FullIncidentDto target = new FullIncidentDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Scenario ->
    // ScenarioDto
    public FullScenarioDto transformScenario2Dto(@Valid Scenario source) {
        FullScenarioDto target = new FullScenarioDto();
        mapCompositeEntity(source, target);
        return target;
    }

    // Scope ->
    // ScopeDto
    public FullScopeDto transformScope2Dto(@Valid Scope source) {
        FullScopeDto target = new FullScopeDto();
        mapEntityLayerSupertype(source, target);
        target.setMembers(convertReferenceSet(source.getMembers()));
        return target;
    }

    // Domain ->
    // DomainDto
    public FullDomainDto transformDomain2Dto(@Valid Domain source) {
        var target = new FullDomainDto();
        target.setId(source.getId()
                           .uuidValue());
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);
        return target;
    }

    public FullCatalogDto transformCatalog2Dto(@Valid Catalog source) {
        FullCatalogDto target = new FullCatalogDto();

        target.setId(source.getId()
                           .uuidValue());
        mapNameableProperties(source, target);
        mapVersionedProperties(source, target);

        if (source.getDomainTemplate() != null) {
            target.setDomainTemplate(ModelObjectReference.from(source.getDomainTemplate(),
                                                               referenceAssembler));
        }
        target.setCatalogItems(convertReferenceSet(source.getCatalogItems()));

        return target;
    }

    public FullCatalogItemDto transformCatalogItem2Dto(@Valid CatalogItem source) {
        FullCatalogItemDto target = new FullCatalogItemDto();

        target.setId(source.getId()
                           .uuidValue());
        mapVersionedProperties(source, target);
        target.setNamespace(source.getNamespace());
        if (source.getCatalog() != null) {
            target.setCatalog(ModelObjectReference.from(source.getCatalog(), referenceAssembler));
        }

        if (source.getElement() != null) {
            target.setElement(ModelObjectReference.from(source.getElement(), referenceAssembler));
        }
        target.setTailoringReferences(source.getTailoringReferences()
                                            .stream()
                                            .map(this::transformTailoringReference2Dto)
                                            .collect(Collectors.toSet())

        );
        return target;
    }

    public FullTailoringReferenceDto transformTailoringReference2Dto(
            @Valid TailoringReference source) {
        FullTailoringReferenceDto target = new FullTailoringReferenceDto();
        mapVersionedProperties(source, target);
        target.setId(source.getId()
                           .uuidValue());

        target.setReferenceType(source.getReferenceType());

        if (source.getCatalogItem() != null) {
            target.setCatalogItem(ModelObjectReference.from(source.getCatalogItem(),
                                                            referenceAssembler));
        }
        return target;
    }

    // Unit -> UnitDto
    public FullUnitDto transformUnit2Dto(@Valid Unit source) {
        var target = new FullUnitDto();
        target.setId(source.getId()
                           .uuidValue());
        target.setVersion(source.getVersion());
        target.setUnits(convertSet(source.getUnits(),
                                   u -> ModelObjectReference.from(u, referenceAssembler)));
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);

        target.setDomains(convertReferenceSet(source.getDomains()));
        if (source.getClient() != null) {
            target.setClient(ModelObjectReference.from(source.getClient(), referenceAssembler));
        }
        if (source.getParent() != null) {
            target.setParent(ModelObjectReference.from(source.getParent(), referenceAssembler));
        }

        return target;
    }

    // CustomLink ->
    // CustomLinkDto
    public CustomLinkDto transformCustomLink2Dto(@Valid CustomLink source) {
        var target = new CustomLinkDto();
        // Copying the set triggers lazy loading right here while the
        // transaction is
        // still open, avoiding a LazyInit exception during serialization.
        // TODO VEO-448 Join fetch applicableTo.
        target.setApplicableTo(new HashSet<>(source.getApplicableTo()));
        target.setName(source.getName());

        target.setAttributes(source.getAllProperties());

        if (source.getTarget() != null) {
            target.setTarget(ModelObjectReference.from(source.getTarget(), referenceAssembler));
        }

        return target;

    }

    private <TDto extends EntityLayerSupertypeDto & IdentifiableDto> void mapEntityLayerSupertype(
            EntityLayerSupertype source, TDto target) {
        target.setId(source.getId()
                           .uuidValue());
        target.setVersion(source.getVersion());
        mapVersionedProperties(source, target);
        mapNameableProperties(source, target);

        target.setDomains(convertReferenceSet(source.getDomains()));
        target.setLinks(mapLinks(source.getLinks()));
        target.setCustomAspects(mapCustomAspects(source.getCustomAspects()));
        target.setType(source.getModelType());
        subTypeTransformer.mapSubTypesToDto(source, target);

        if (source.getOwner() != null) {
            target.setOwner(ModelObjectReference.from(source.getOwner(), referenceAssembler));
        }
    }

    private <TEntity extends EntityLayerSupertype, TDto extends CompositeEntityDto<TEntity> & IdentifiableDto> void mapCompositeEntity(
            CompositeEntity<TEntity> source, TDto target) {
        mapEntityLayerSupertype(source, target);
        target.setParts(convertReferenceSet(source.getParts()));
    }

    // CustomProperties ->
    // CustomPropertiesDto
    public CustomPropertiesDto transformCustomProperties2Dto(@Valid CustomProperties source) {
        var target = new CustomPropertiesDto();
        // Copying the set triggers lazy loading right here while the
        // transaction is
        // still open, avoiding a LazyInit exception during serialization.
        // TODO VEO-448 Join fetch applicableTo.
        target.setApplicableTo(new HashSet<>(source.getApplicableTo()));

        target.setAttributes(source.getAllProperties());
        return target;
    }

    private static void mapNameableProperties(Nameable source, NameableDto target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static void mapVersionedProperties(Versioned source, VersionedDto target) {
        target.setCreatedAt(source.getCreatedAt()
                                  .toString());
        target.setCreatedBy(source.getCreatedBy());
        target.setUpdatedAt(source.getUpdatedAt()
                                  .toString());
        target.setUpdatedBy(source.getUpdatedBy());
    }

    private static <TIn, TOut> Set<TOut> convertSet(Set<TIn> input, Function<TIn, TOut> mapper) {
        return input.stream()
                    .map(mapper)
                    .collect(Collectors.toSet());
    }

    private <T extends ModelObject> Set<ModelObjectReference<T>> convertReferenceSet(
            Set<T> domains) {
        return domains.stream()
                      .map(o -> ModelObjectReference.from(o, referenceAssembler))
                      .collect(Collectors.toSet());
    }

    private Map<String, List<CustomLinkDto>> mapLinks(Set<CustomLink> links) {
        return links.stream()
                    .collect(groupingBy(CustomLink::getType))
                    .entrySet()
                    .stream()
                    .collect(toMap(Map.Entry::getKey, entry -> entry.getValue()
                                                                    .stream()
                                                                    .map(link -> transformCustomLink2Dto(link))
                                                                    .collect(toList())));
    }

    private Map<String, CustomPropertiesDto> mapCustomAspects(Set<CustomProperties> customAspects) {
        return customAspects.stream()
                            .collect(toMap(CustomProperties::getType,
                                           this::transformCustomProperties2Dto));
    }
}
