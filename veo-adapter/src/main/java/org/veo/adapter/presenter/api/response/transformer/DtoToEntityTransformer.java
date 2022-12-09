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

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractCatalogItemDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainTemplateDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractIncidentDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.dto.AbstractScenarioDto;
import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractUnitDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.composite.CompositeCatalogDto;
import org.veo.adapter.presenter.api.dto.composite.CompositeCatalogItemDto;
import org.veo.adapter.presenter.api.dto.reference.ReferenceCatalogDto;
import org.veo.adapter.presenter.api.dto.reference.ReferenceCatalogItemDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformLinkTailoringReference;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Catalog;
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
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;

import lombok.RequiredArgsConstructor;

/** A collection of transform functions to transform entities to Dto back and forth. */
@RequiredArgsConstructor
public final class DtoToEntityTransformer {

  private final EntityFactory factory;
  private final IdentifiableFactory identifiableFactory;
  private final DomainAssociationTransformer domainAssociationTransformer;

  public Person transformDto2Person(AbstractPersonDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Person.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Asset transformDto2Asset(AbstractAssetDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Asset.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Process transformDto2Process(AbstractProcessDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Process.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Document transformDto2Document(AbstractDocumentDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Document.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Control transformDto2Control(AbstractControlDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Control.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Incident transformDto2Incident(AbstractIncidentDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Incident.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Scenario transformDto2Scenario(AbstractScenarioDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Scenario.class, source);
    mapCompositeEntity(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    return target;
  }

  public Scope transformDto2Scope(AbstractScopeDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Scope.class, source);
    mapElement(source, target, idRefResolver);
    domainAssociationTransformer.mapDomainsToEntity(source, target, idRefResolver);
    Set<IdRef<Element>> memberReferences = source.getMembers();
    Map<Class<Element>, Set<IdRef<Element>>> memberReferencesByType =
        memberReferences.stream()
            .collect(Collectors.groupingBy(IdRef::getType, Collectors.toSet()));
    Set<Element> members =
        memberReferencesByType.values().stream()
            .flatMap(refs -> idRefResolver.resolve(refs).stream())
            .collect(Collectors.toSet());

    target.setMembers(members);
    return target;
  }

  public DomainTemplate transformTransformDomainTemplateDto2DomainTemplate(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(DomainTemplate.class, source);
    mapTransformDomainTemplate(source, idRefResolver, target);
    return target;
  }

  public Domain transformTransformDomainTemplateDto2Domain(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver) {
    // DO NOT use domain template ID as domain ID.
    var target = identifiableFactory.create(Domain.class, Key.newUuid());
    mapTransformDomainTemplate(source, idRefResolver, target);
    target.setActive(true);
    return target;
  }

  private void mapTransformDomainTemplate(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver, DomainBase target) {
    mapDomainTemplate(source, idRefResolver, target);

    target.setElementTypeDefinitions(
        source.getElementTypeDefinitions().entrySet().stream()
            .map(entry -> mapElementTypeDefinition(entry.getKey(), entry.getValue(), target))
            .collect(Collectors.toSet()));
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));
  }

  public ElementTypeDefinition mapElementTypeDefinition(
      String type, ElementTypeDefinitionDto source, DomainBase owner) {
    var target = factory.createElementTypeDefinition(type, owner);
    target.setSubTypes(source.getSubTypes());
    target.setCustomAspects(source.getCustomAspects());
    target.setLinks(source.getLinks());
    target.setTranslations(source.getTranslations());
    return target;
  }

  public Catalog transformDto2Catalog(AbstractCatalogDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Catalog.class, source);
    idRefResolver.resolve(source.getDomainTemplate()).addToCatalogs(target);
    mapNameableProperties(source, target);
    if (source instanceof ReferenceCatalogDto catalogDto) {
      target.setCatalogItems(idRefResolver.resolve(catalogDto.getCatalogItems()));
    } else if (source instanceof CompositeCatalogDto catalogDto) {
      target.setCatalogItems(
          convertSet(
              catalogDto.getCatalogItems(),
              ci -> transformDto2CatalogItem(ci, idRefResolver, target)));
    }

    return target;
  }

  public TailoringReference transformDto2TailoringReference(
      AbstractTailoringReferenceDto source, CatalogItem owner, IdRefResolver idRefResolver) {

    var target =
        source.isLinkTailoringReferences()
            ? createIdentifiable(LinkTailoringReference.class, source)
            : createIdentifiable(TailoringReference.class, source);
    target.setOwner(owner);
    target.setReferenceType(source.getReferenceType());
    if (source.getCatalogItem() != null) {
      CatalogItem resolve = idRefResolver.resolve(source.getCatalogItem());
      target.setCatalogItem(resolve);
    }

    if (source.isLinkTailoringReferences()) {
      TransformLinkTailoringReference tailoringReferenceDto =
          (TransformLinkTailoringReference) source;
      LinkTailoringReference tailoringReference = (LinkTailoringReference) target;
      tailoringReference.setAttributes(tailoringReferenceDto.getAttributes());
      tailoringReference.setLinkType(tailoringReferenceDto.getLinkType());
    }

    return target;
  }

  public Unit transformDto2Unit(AbstractUnitDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Unit.class, source);
    mapNameableProperties(source, target);

    target.setDomains(idRefResolver.resolve(source.getDomains()));
    if (source.getParent() != null) {
      target.setParent(idRefResolver.resolve(source.getParent()));
    }

    return target;
  }

  public CustomLink transformDto2CustomLink(
      CustomLinkDto source, String type, IdRefResolver idRefResolver) {
    Element linkTarget = null;
    if (source.getTarget() != null) {
      linkTarget = idRefResolver.resolve(source.getTarget());
    }

    var target = factory.createCustomLink(linkTarget, null, type);

    target.setAttributes(source.getAttributes());
    return target;
  }

  public CustomAspect transformDto2CustomAspect(
      EntityFactory factory, CustomAspectDto source, String type) {
    var target = factory.createCustomAspect(type);
    target.setAttributes(source.getAttributes());
    return target;
  }

  public Element transformDto2Element(AbstractElementDto elementDto, IdRefResolver idRefResolver) {
    if (elementDto instanceof AbstractAssetDto asset) {
      return transformDto2Asset(asset, idRefResolver);
    } else if (elementDto instanceof AbstractControlDto control) {
      return transformDto2Control(control, idRefResolver);
    } else if (elementDto instanceof AbstractDocumentDto document) {
      return transformDto2Document(document, idRefResolver);
    } else if (elementDto instanceof AbstractIncidentDto incdent) {
      return transformDto2Incident(incdent, idRefResolver);
    } else if (elementDto instanceof AbstractPersonDto person) {
      return transformDto2Person(person, idRefResolver);
    } else if (elementDto instanceof AbstractProcessDto process) {
      return transformDto2Process(process, idRefResolver);
    } else if (elementDto instanceof AbstractScenarioDto scenario) {
      return transformDto2Scenario(scenario, idRefResolver);
    } else if (elementDto instanceof AbstractScopeDto scope) {
      return transformDto2Scope(scope, idRefResolver);
    }
    throw new IllegalArgumentException("unkown type: " + elementDto.getClass().getName());
  }

  private void mapDomainTemplate(
      AbstractDomainTemplateDto source, IdRefResolver idRefResolver, DomainBase target) {
    target.setAuthority(source.getAuthority());
    target.setTemplateVersion(source.getTemplateVersion());
    target.setProfiles(Map.copyOf(source.getProfiles()));

    mapNameableProperties(source, target);
    if (source.getCatalogs() != null) {
      target.setCatalogs(
          source.getCatalogs().stream()
              .map(c -> transformDto2Catalog(c, idRefResolver))
              .collect(Collectors.toSet()));
    }
    target.setRiskDefinitions(Map.copyOf(source.getRiskDefinitions()));
  }

  private <T extends CompositeElement> void mapCompositeEntity(
      CompositeEntityDto<T> source, CompositeElement<T> target, IdRefResolver idRefResolver) {
    mapElement(source, target, idRefResolver);
    target.setParts(idRefResolver.resolve(source.getParts()));
  }

  private <TDto extends AbstractElementDto, TEntity extends Element> void mapElement(
      TDto source, TEntity target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);
    target.setLinks(mapLinks(target, source, idRefResolver));
    target.setCustomAspects(mapCustomAspects(source, factory));
    if (source.getOwner() != null) {
      target.setOwnerOrContainingCatalogItem(idRefResolver.resolve(source.getOwner()));
    }
  }

  private Set<CustomLink> mapLinks(
      Element entity, AbstractElementDto dto, IdRefResolver idRefResolver) {
    return dto.getLinks().entrySet().stream()
        .flatMap(
            entry ->
                entry.getValue().stream()
                    .map(
                        linktDto -> {
                          var customLink =
                              transformDto2CustomLink(linktDto, entry.getKey(), idRefResolver);
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

  private static <TIn, TOut extends Identifiable> Set<TOut> convertSet(
      Set<TIn> source, Function<TIn, TOut> mapper) {
    if (mapper != null) {
      return source.stream().map(mapper).collect(Collectors.toSet());
    }
    return new HashSet<>();
  }

  private Set<CustomAspect> mapCustomAspects(AbstractElementDto dto, EntityFactory factory) {
    return dto.getCustomAspects().entrySet().stream()
        .map(entry -> transformDto2CustomAspect(factory, entry.getValue(), entry.getKey()))
        .collect(Collectors.toSet());
  }

  public CatalogItem transformDto2CatalogItem(
      AbstractCatalogItemDto source, IdRefResolver idRefResolver, Catalog catalog) {
    var target = createIdentifiable(CatalogItem.class, source);
    target.setCatalog(catalog);
    if (source instanceof CompositeCatalogItemDto catalogitem) {
      AbstractElementDto elementDto = catalogitem.getElement();
      if (elementDto instanceof AbstractAssetDto asset) {
        target.setElement(transformDto2Asset(asset, idRefResolver));
      } else if (elementDto instanceof AbstractControlDto control) {
        target.setElement(transformDto2Control(control, idRefResolver));
      } else if (elementDto instanceof AbstractDocumentDto document) {
        target.setElement(transformDto2Document(document, idRefResolver));
      } else if (elementDto instanceof AbstractIncidentDto incident) {
        target.setElement(transformDto2Incident(incident, idRefResolver));
      } else if (elementDto instanceof AbstractPersonDto person) {
        target.setElement(transformDto2Person(person, idRefResolver));
      } else if (elementDto instanceof AbstractProcessDto process) {
        target.setElement(transformDto2Process(process, idRefResolver));
      } else if (elementDto instanceof AbstractScenarioDto scenario) {
        target.setElement(transformDto2Scenario(scenario, idRefResolver));
      } else if (elementDto instanceof AbstractScopeDto scope) {
        target.setElement(transformDto2Scope(scope, idRefResolver));
      }
    } else if (source instanceof ReferenceCatalogItemDto catalogitem) {
      target.setElement(idRefResolver.resolve(catalogitem.getElement()));
    } else {
      throw new IllegalArgumentException(
          "Cannot handle entity type " + source.getClass().getName());
    }
    target.setNamespace(source.getNamespace());

    target.getTailoringReferences().clear();
    target
        .getTailoringReferences()
        .addAll(
            convertSet(
                source.getTailoringReferences(),
                tr -> transformDto2TailoringReference(tr, target, idRefResolver)));
    return target;
  }

  private <T extends Identifiable> T createIdentifiable(Class<T> type, Object source) {
    Key<UUID> key = null;
    if (source instanceof IdentifiableDto identifiable) {
      key = Key.uuidFrom(identifiable.getId());
    }
    return identifiableFactory.create(type, key);
  }
}
