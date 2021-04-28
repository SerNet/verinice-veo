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

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractCatalogItemDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainTemplateDto;
import org.veo.adapter.presenter.api.dto.AbstractIncidentDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.dto.AbstractScenarioDto;
import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractUnitDto;
import org.veo.adapter.presenter.api.dto.CatalogableDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.VersionedDto;
import org.veo.adapter.presenter.api.dto.composite.CompositeCatalogDto;
import org.veo.adapter.presenter.api.dto.composite.CompositeCatalogItemDto;
import org.veo.adapter.presenter.api.dto.reference.ReferenceCatalogDto;
import org.veo.adapter.presenter.api.dto.reference.ReferenceCatalogItemDto;
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
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.EntityFactory;

/**
 * A collection of transform functions to transform entities to Dto back and
 * forth.
 */
public final class DtoToEntityTransformer {

    public DtoToEntityTransformer(EntityFactory entityFactory,
            EntitySchemaLoader entitySchemaLoader, SubTypeTransformer subTypeTransformer) {
        this.factory = entityFactory;
        this.entitySchemaLoader = entitySchemaLoader;
        this.subTypeTransformer = subTypeTransformer;
    }

    private final EntityFactory factory;
    private final EntitySchemaLoader entitySchemaLoader;
    private final SubTypeTransformer subTypeTransformer;

    // PersonDto->Person
    public Person transformDto2Person(AbstractPersonDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createPerson(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // AssetDto->Asset
    public Asset transformDto2Asset(AbstractAssetDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createAsset(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // ProcessDto->Process
    public Process transformDto2Process(AbstractProcessDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createProcess(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // DocumentDto->Document
    public Document transformDto2Document(AbstractDocumentDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createDocument(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // ControlDto->Control
    public Control transformDto2Control(AbstractControlDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createControl(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // IncidentDto->Incident
    public Incident transformDto2Incident(AbstractIncidentDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createIncident(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    // ScenarioDto->Scenario
    public Scenario transformDto2Scenario(AbstractScenarioDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createScenario(source.getName(), null);
        mapCompositeEntity(source, target, modelObjectReferenceResolver);
        return target;
    }

    public Scope transformDto2Scope(AbstractScopeDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createScope(source.getName(), null);
        mapEntityLayerSupertype(source, target, modelObjectReferenceResolver);
        Set<ModelObjectReference<EntityLayerSupertype>> memberReferences = source.getMembers();
        Map<Class<EntityLayerSupertype>, Set<ModelObjectReference<EntityLayerSupertype>>> memberReferencesByType = memberReferences.stream()
                                                                                                                                   .collect(Collectors.groupingBy(ModelObjectReference::getType,
                                                                                                                                                                  Collectors.toSet()));
        Set<EntityLayerSupertype> members = memberReferencesByType.values()
                                                                  .stream()
                                                                  .flatMap(refs -> modelObjectReferenceResolver.resolve(refs)
                                                                                                               .stream())
                                                                  .collect(Collectors.toSet());

        target.setMembers(members);
        return target;
    }

    // DomainDto->Domain
    public Domain transformDto2Domain(AbstractDomainDto source, Key<UUID> key) {
        var target = factory.createDomain(source.getName(), "", "", "");
        mapIdentifiableProperties(source, target);
        mapNameableProperties(source, target);
        target.setActive(true);

        return target;
    }

    public Domain transformDomainTemplateDto2Domain(AbstractDomainTemplateDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createDomain(source.getName(), source.getAuthority(),
                                          source.getTemplateVersion(), source.getRevision());
        target.setActive(true);
        mapDomainTemplate(source, modelObjectReferenceResolver, target);
        return target;
    }

    public DomainTemplate transformDto2DomainTemplate(AbstractDomainTemplateDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createDomainTemplate(source.getName(), source.getAuthority(),
                                                  source.getTemplateVersion(), source.getRevision(),
                                                  null);
        mapIdentifiableProperties(source, target);
        mapDomainTemplate(source, modelObjectReferenceResolver, target);

        return target;
    }

    public Catalog transformDto2Catalog(AbstractCatalogDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createCatalog(modelObjectReferenceResolver.resolve(source.getDomainTemplate()));
        mapIdentifiableProperties(source, target);
        mapNameableProperties(source, target);
        if (source instanceof ReferenceCatalogDto) {
            ReferenceCatalogDto catalogDto = (ReferenceCatalogDto) source;
            target.setCatalogItems(modelObjectReferenceResolver.resolve(catalogDto.getCatalogItems()));
        } else if (source instanceof CompositeCatalogDto) {
            CompositeCatalogDto catalogDto = (CompositeCatalogDto) source;
            target.setCatalogItems(convertSet(catalogDto.getCatalogItems(),
                                              ci -> transformDto2CatalogItem(ci,
                                                                             modelObjectReferenceResolver,
                                                                             target)));
        }

        return target;
    }

    public TailoringReference transformDto2TailoringReference(AbstractTailoringReferenceDto source,
            CatalogItem owner, ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createTailoringReference(owner);
        mapIdentifiableProperties(source, target);
        if (source.getCatalogItem() != null) {
            CatalogItem resolve = modelObjectReferenceResolver.resolve(source.getCatalogItem());
            target.setCatalogItem(resolve);
        }
        target.setReferenceType(source.getReferenceType());
        return target;
    }

    // UnitDto->Unit
    public Unit transformDto2Unit(AbstractUnitDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        var target = factory.createUnit(source.getName(), null);
        mapIdentifiableProperties(source, target);
        mapNameableProperties(source, target);

        target.setDomains(modelObjectReferenceResolver.resolve(source.getDomains()));
        if (source.getClient() != null) {
            target.setClient(modelObjectReferenceResolver.resolve(source.getClient()));
        }
        if (source.getParent() != null) {
            target.setParent(modelObjectReferenceResolver.resolve(source.getParent()));
        }

        return target;
    }

    // CustomLinkDto->CustomLink
    public CustomLink transformDto2CustomLink(CustomLinkDto source, String type,
            EntitySchema entitySchema, ModelObjectReferenceResolver modelObjectReferenceResolver) {
        EntityLayerSupertype linkTarget = null;
        if (source.getTarget() != null) {
            linkTarget = modelObjectReferenceResolver.resolve(source.getTarget());
        }

        var target = factory.createCustomLink(source.getName(), linkTarget, null);

        target.setAttributes(source.getAttributes());
        target.setType(type);
        target.setName(source.getName());
        entitySchema.validateCustomLink(target);
        return target;

    }

    // CustomPropertiesDto->CustomProperties
    public CustomProperties transformDto2CustomProperties(EntityFactory factory,
            CustomPropertiesDto source, String type, EntitySchema entitySchema) {
        var target = factory.createCustomProperties();
        target.setAttributes(source.getAttributes());
        target.setType(type);
        entitySchema.validateCustomAspect(target);
        return target;
    }

    private void mapDomainTemplate(AbstractDomainTemplateDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver, DomainTemplate target) {
        mapNameableProperties(source, target);
        if (source.getCatalogs() != null) {
            target.setCatalogs(source.getCatalogs()
                                     .stream()
                                     .map(c -> transformDto2Catalog(c,
                                                                    modelObjectReferenceResolver))
                                     .collect(Collectors.toSet())

            );
        }
    }

    private <T extends EntityLayerSupertype> void mapCompositeEntity(CompositeEntityDto<T> source,
            CompositeEntity<T> target, ModelObjectReferenceResolver modelObjectReferenceResolver) {
        mapEntityLayerSupertype(source, target, modelObjectReferenceResolver);
        target.setParts(modelObjectReferenceResolver.resolve(source.getParts()));
    }

    private <TDto extends EntityLayerSupertypeDto, TEntity extends EntityLayerSupertype> void mapEntityLayerSupertype(
            TDto source, TEntity target,
            ModelObjectReferenceResolver modelObjectReferenceResolver) {
        mapIdentifiableProperties(source, target);
        mapNameableProperties(source, target);
        target.setDomains(modelObjectReferenceResolver.resolve(source.getDomains()));
        subTypeTransformer.mapSubTypesToEntity(source, target);
        var entitySchema = loadEntitySchema(target.getModelType());
        target.setLinks(mapLinks(target, source, entitySchema, modelObjectReferenceResolver));
        target.setCustomAspects(mapCustomAspects(source, factory, entitySchema));
        if (source.getOwner() != null) {
            target.setOwner(modelObjectReferenceResolver.resolve(source.getOwner()));
        }
    }

    private void mapIdentifiableProperties(VersionedDto source, ModelObject target) {
        if (source instanceof IdentifiableDto) {
            target.setId(Key.uuidFrom(((IdentifiableDto) source).getId()));
        }
    }

    private Set<CustomLink> mapLinks(EntityLayerSupertype entity, EntityLayerSupertypeDto dto,
            EntitySchema entitySchema, ModelObjectReferenceResolver modelObjectReferenceResolver) {
        return dto.getLinks()
                  .entrySet()
                  .stream()
                  .flatMap(entry -> entry.getValue()
                                         .stream()
                                         .map(linktDto -> {
                                             var customLink = transformDto2CustomLink(linktDto,
                                                                                      entry.getKey(),
                                                                                      entitySchema,
                                                                                      modelObjectReferenceResolver);
                                             customLink.setSource(entity);
                                             return customLink;
                                         }))
                  .collect(toSet());
    }

    private static void mapNameableProperties(NameableDto source, Nameable target) {
        target.setName(source.getName());
        target.setAbbreviation(source.getAbbreviation());
        target.setDescription(source.getDescription());
    }

    private static <TIn, TOut extends ModelObject> Set<TOut> convertSet(Set<TIn> source,
            Function<TIn, TOut> mapper) {
        if (mapper != null) {
            return source.stream()
                         .map(mapper)
                         .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private Set<CustomProperties> mapCustomAspects(EntityLayerSupertypeDto dto,
            EntityFactory factory, EntitySchema entitySchema) {
        return dto.getCustomAspects()
                  .entrySet()
                  .stream()
                  .map(entry -> transformDto2CustomProperties(factory, entry.getValue(),
                                                              entry.getKey(), entitySchema))
                  .collect(Collectors.toSet());
    }

    private EntitySchema loadEntitySchema(String entityType) {
        return entitySchemaLoader.load(entityType);
    }

    public CatalogItem transformDto2CatalogItem(AbstractCatalogItemDto source,
            ModelObjectReferenceResolver modelObjectReferenceResolver, Catalog catalog) {
        var target = factory.createCatalogItem(catalog);
        mapIdentifiableProperties(source, target);
        target.setNamespace(source.getNamespace());
        if (source instanceof CompositeCatalogItemDto) {
            CompositeCatalogItemDto catalogitem = (CompositeCatalogItemDto) source;
            CatalogableDto catalogableDto = catalogitem.getElement();
            if (catalogableDto instanceof AbstractAssetDto) {
                target.setElement(transformDto2Asset((AbstractAssetDto) catalogableDto,
                                                     modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractControlDto) {
                target.setElement(transformDto2Control((AbstractControlDto) catalogableDto,
                                                       modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractDocumentDto) {
                target.setElement(transformDto2Document((AbstractDocumentDto) catalogableDto,
                                                        modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractIncidentDto) {
                target.setElement(transformDto2Incident((AbstractIncidentDto) catalogableDto,
                                                        modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractPersonDto) {
                target.setElement(transformDto2Person((AbstractPersonDto) catalogableDto,
                                                      modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractProcessDto) {
                target.setElement(transformDto2Process((AbstractProcessDto) catalogableDto,
                                                       modelObjectReferenceResolver));
            } else if (catalogableDto instanceof AbstractScenarioDto) {
                target.setElement(transformDto2Scenario((AbstractScenarioDto) catalogableDto,
                                                        modelObjectReferenceResolver));
            }
        } else if (source instanceof ReferenceCatalogItemDto) {
            ReferenceCatalogItemDto catalogitem = (ReferenceCatalogItemDto) source;
            target.setElement(modelObjectReferenceResolver.resolve(catalogitem.getElement()));
        }

        if (target.getElement() != null) {
            target.getElement()
                  .setOwner(target);
        }
        target.getTailoringReferences()
              .clear();
        target.getTailoringReferences()
              .addAll(convertSet(source.getTailoringReferences(),
                                 tr -> transformDto2TailoringReference(tr, target,
                                                                       modelObjectReferenceResolver)));
        return target;
    }

}
