/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractScopeDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformElementDto;
import org.veo.core.ExportDto;
import org.veo.core.VeoInputStreamResource;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scope;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateIdGenerator;
import org.veo.core.service.DomainTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainTemplateServiceImpl implements DomainTemplateService {

  private final DomainTemplateRepository domainTemplateRepository;
  private final DtoToEntityTransformer entityTransformer;
  private final EntityToDtoTransformer dtoTransformer;
  private final EntityFactory factory;
  private final List<VeoInputStreamResource> domainResources;
  private final CatalogItemPrepareStrategy preparations;
  private final DomainTemplateIdGenerator domainTemplateIdGenerator;

  private final ReferenceAssembler assembler;
  private final ObjectMapper objectMapper;
  private final Map<String, VeoInputStreamResource> domainTemplateFiles = new HashMap<>();

  public DomainTemplateServiceImpl(
      DomainTemplateRepository domainTemplateRepository,
      EntityFactory factory,
      List<VeoInputStreamResource> domainResources,
      DomainAssociationTransformer domainAssociationTransformer,
      IdentifiableFactory identifiableFactory,
      CatalogItemPrepareStrategy preparations,
      DomainTemplateIdGenerator domainTemplateIdGenerator) {
    this.domainTemplateRepository = domainTemplateRepository;
    this.factory = factory;
    this.domainResources = domainResources;
    this.preparations = preparations;
    this.domainTemplateIdGenerator = domainTemplateIdGenerator;

    entityTransformer =
        new DtoToEntityTransformer(factory, identifiableFactory, domainAssociationTransformer);
    assembler = new LocalReferenceAssembler();
    dtoTransformer = new EntityToDtoTransformer(assembler, domainAssociationTransformer);
    objectMapper =
        new ObjectMapper()
            .addMixIn(AbstractElementDto.class, TransformElementDto.class)
            .registerModule(
                new SimpleModule()
                    .addDeserializer(IdRef.class, new ReferenceDeserializer(assembler)))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    readTemplateFiles();
  }

  private void readTemplateFiles() {
    log.info("read files from resources ...");
    domainResources.forEach(
        f -> {
          log.info("read file:{}", f.getDescription());
          TransformDomainTemplateDto templateDto = null;
          try {
            templateDto = readInstanceFile(f);
            domainTemplateFiles.put(templateDto.getId(), f);
          } catch (IOException e) {
            log.error("Error reading file", e);
          }
        });
  }

  @Override
  public List<DomainTemplate> getTemplates(Client client) {
    return Collections.emptyList();
  }

  @Override
  public Optional<DomainTemplate> getTemplate(Client client, Key<UUID> key) {
    return Optional.empty();
  }

  @Override
  public Domain createDomain(Client client, String templateId) {
    DomainTemplate domainTemplate =
        domainTemplateRepository
            .findById(Key.uuidFrom(templateId))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Domain template %s does not exist for client %s.", templateId, client));

    TransformDomainTemplateDto domainTemplateDto =
        dtoTransformer.transformDomainTemplate2Dto(domainTemplate);
    Domain domainPlaceholder =
        factory.createDomain(
            domainTemplateDto.getName(),
            domainTemplateDto.getAuthority(),
            domainTemplateDto.getTemplateVersion(),
            domainTemplateDto.getRevision());
    Domain domain = processDomainTemplate(domainTemplateDto, domainPlaceholder);
    domain.setDomainTemplate(domainTemplate);
    client.addToDomains(domain);
    log.info("Domain {} created for client {}", domain.getName(), client);
    return domain;
  }

  @Override
  public Collection<Element> getElementsForDemoUnit(Client client) {
    log.info("Create demo unit elements for {}", client);
    Set<Element> elements = new HashSet<>();
    PlaceholderResolver ref = new PlaceholderResolver(entityTransformer);
    client.getDomains().stream()
        .filter(
            d ->
                d.isActive()
                    && d.getDomainTemplate() != null
                    && domainTemplateFiles.containsKey(d.getDomainTemplate().getIdAsString()))
        .forEach(
            domain -> {
              log.info("Processing {}", domain);
              DomainTemplate template = domain.getDomainTemplate();
              // TODO VEO-1168: read demo unit elements from the database
              VeoInputStreamResource templateFile =
                  domainTemplateFiles.get(template.getIdAsString());
              try {
                TransformDomainTemplateDto domainTemplateDto = readInstanceFile(templateFile);
                ref.cache.put(domainTemplateDto.getId(), domain);
                Map<String, Element> elementCache =
                    createElementCache(
                        ref,
                        domainTemplateDto.getDemoUnitElements().stream()
                            .map(this::removeOwner)
                            .map(IdentifiableDto.class::cast)
                            .collect(
                                Collectors.toMap(IdentifiableDto::getId, Function.identity())));

                elementCache
                    .entrySet()
                    .forEach(
                        e -> {
                          Element value = e.getValue();
                          value.setId(null);
                        });
                elements.addAll(elementCache.values());
              } catch (JsonMappingException e) {
                log.error("Error parsing file", e);
              } catch (IOException e) {
                log.error("Error loading file", e);
              }
            });
    return elements;
  }
  /**
   * This will bootstrap the data in the db, the domain template is created and inserted in or
   * returned from the database. We currently store the basic data.
   */
  public DomainTemplate createDomainTemplate(String id, VeoInputStreamResource templateFile) {
    Optional<DomainTemplate> dt = domainTemplateRepository.findById(Key.uuidFrom(id));
    if (dt.isPresent()) {
      return dt.get();
    }
    try {
      TransformDomainTemplateDto domainTemplateDto = readInstanceFile(templateFile);
      DomainTemplate newDomainTemplate =
          factory.createDomainTemplate(
              domainTemplateDto.getName(),
              domainTemplateDto.getAuthority(),
              domainTemplateDto.getTemplateVersion(),
              domainTemplateDto.getRevision(),
              Key.uuidFrom(id));

      processDomainTemplate(domainTemplateDto, newDomainTemplate);
      preparations.updateVersion(newDomainTemplate);
      log.info("Create and save domain template {}", newDomainTemplate);
      return domainTemplateRepository.save(newDomainTemplate);
    } catch (JsonMappingException e) {
      log.error("Error parsing file", e);
    } catch (IOException e) {
      log.error("Error loading file", e);
    }
    throw new NotFoundException("Domain template %s file %s not found.", id, templateFile);
  }

  @Override
  public DomainTemplate createDomainTemplateFromDomain(Domain domain) {
    TransformDomainTemplateDto domainTemplateDto =
        dtoTransformer.transformDomainTemplate2Dto(domain);

    String domainTemplateId = createDomainTemplateId(domain);
    Key<UUID> domainTemplateKey = Key.uuidFrom(domainTemplateId);
    if (domainTemplateRepository.exists(domainTemplateKey)) {
      throw new ModelConsistencyException("The UUID %s is already used.", domainTemplateId);
    }
    DomainTemplate newDomainTemplate =
        factory.createDomainTemplate(
            domainTemplateDto.getName(),
            domainTemplateDto.getAuthority(),
            domainTemplateDto.getTemplateVersion(),
            domainTemplateDto.getRevision(),
            domainTemplateKey);

    processDomainTemplate(domainTemplateDto, newDomainTemplate);
    preparations.updateVersion(newDomainTemplate);
    log.info("Create and save domain template {} from domain {}", newDomainTemplate, domain);
    return domainTemplateRepository.save(newDomainTemplate);
  }

  private String createDomainTemplateId(Domain domain) {
    return domainTemplateIdGenerator.createDomainTemplateId(
        domain.getName(), domain.getTemplateVersion(), domain.getRevision());
  }

  /** Transform the given domainTemplateDto to a new domain. */
  private DomainTemplate processDomainTemplate(
      TransformDomainTemplateDto domainTemplateDto, DomainTemplate newDomain) {
    newDomain.setDescription(domainTemplateDto.getDescription());
    newDomain.setAbbreviation(domainTemplateDto.getAbbreviation());
    PlaceholderResolver ref = new PlaceholderResolver(entityTransformer);

    ref.cache.put(domainTemplateDto.getId(), newDomain);

    domainTemplateDto.getCatalogs().stream()
        .forEach(
            c -> {
              Catalog createCatalog = factory.createCatalog(newDomain);
              createCatalog.setName(c.getName());
              createCatalog.setAbbreviation(c.getAbbreviation());
              createCatalog.setDescription(c.getDescription());
              preparations.updateVersion(createCatalog);
              ref.cache.put(((IdentifiableDto) c).getId(), createCatalog);
              c.setDomainTemplate(
                  new SyntheticIdRef<>(domainTemplateDto.getId(), DomainTemplate.class, assembler));
            });

    Map<String, Element> elementCache =
        createElementCacheFromDomainTemplateDto(domainTemplateDto, ref);
    Map<String, CatalogItem> itemCache =
        createCatalogItemCache(domainTemplateDto, ref, elementCache);

    domainTemplateDto.getCatalogs().stream()
        .map(TransformCatalogDto.class::cast)
        .forEach(
            c ->
                c.getCatalogItems()
                    .forEach(
                        i ->
                            entityTransformer.transformDto2CatalogItem(
                                i, ref, (Catalog) ref.cache.get(c.getId()))));
    domainTemplateDto.getElementTypeDefinitions().entrySet().stream()
        .map(
            entry ->
                entityTransformer.mapElementTypeDefinition(
                    entry.getKey(), entry.getValue(), newDomain))
        .forEach(etd -> newDomain.getElementTypeDefinitions().add(etd));
    newDomain.setRiskDefinitions(Map.copyOf(domainTemplateDto.getRiskDefinitions()));

    initCatalog(newDomain, itemCache);
    return newDomain;
  }

  private Domain processDomainTemplate(
      TransformDomainTemplateDto domainTemplateDto, Domain newDomain) {
    newDomain.setDescription(domainTemplateDto.getDescription());
    newDomain.setAbbreviation(domainTemplateDto.getAbbreviation());
    PlaceholderResolver ref = new PlaceholderResolver(entityTransformer);

    ref.cache.put(domainTemplateDto.getId(), newDomain);

    domainTemplateDto.getCatalogs().stream()
        .forEach(
            c -> {
              Catalog createCatalog = factory.createCatalog(newDomain);
              ref.cache.put(((IdentifiableDto) c).getId(), createCatalog);
              c.setDomainTemplate(
                  new SyntheticIdRef<>(domainTemplateDto.getId(), DomainTemplate.class, assembler));
            });

    Map<String, Element> elementCache =
        createElementCacheFromDomainTemplateDto(domainTemplateDto, ref);
    Map<String, CatalogItem> itemCache =
        createCatalogItemCache(domainTemplateDto, ref, elementCache);
    Domain domain =
        entityTransformer.transformTransformDomainTemplateDto2Domain(domainTemplateDto, ref);
    initCatalog(domain, itemCache);
    return domain;
  }

  private void initCatalog(DomainTemplate newDomain, Map<String, CatalogItem> itemCache) {
    newDomain
        .getCatalogs()
        .forEach(
            catalog -> {
              catalog.setId(null);
              catalog.setDomainTemplate(newDomain);
              Set<CatalogItem> catalogItems =
                  catalog.getCatalogItems().stream()
                      .map(ci -> itemCache.get(ci.getIdAsString()))
                      .collect(Collectors.toSet());
              catalog.getCatalogItems().clear();
              catalog.getCatalogItems().addAll(catalogItems);
              catalog
                  .getCatalogItems()
                  .forEach(item -> preparations.prepareCatalogItem(newDomain, catalog, item));
            });
  }

  private Map<String, CatalogItem> createCatalogItemCache(
      TransformDomainTemplateDto domainTemplateDto,
      PlaceholderResolver ref,
      Map<String, Element> elementCache) {
    domainTemplateDto.getCatalogs().stream()
        .map(TransformCatalogDto.class::cast)
        .flatMap(c -> c.getCatalogItems().stream())
        .map(TransformCatalogItemDto.class::cast)
        .map(ci -> transformCatalogItem(ci, elementCache, ref))
        .forEach(c -> ref.cache.put(c.getIdAsString(), c));

    Map<String, CatalogItem> itemCache =
        ref.cache.entrySet().stream()
            .filter(e -> (e.getValue() instanceof CatalogItem))
            .collect(Collectors.toMap(Entry::getKey, e -> (CatalogItem) e.getValue()));

    domainTemplateDto.getCatalogs().stream()
        .map(TransformCatalogDto.class::cast)
        .flatMap(c -> c.getCatalogItems().stream())
        .map(TransformCatalogItemDto.class::cast)
        .forEach(ci -> transformTailorRef(ci, itemCache, ref));
    return itemCache;
  }

  private Map<String, Element> createElementCacheFromDomainTemplateDto(
      TransformDomainTemplateDto domainTemplateDto, PlaceholderResolver ref) {
    Map<String, Element> elementCache =
        createElementCache(
            ref,
            domainTemplateDto.getCatalogs().stream()
                .map(TransformCatalogDto.class::cast)
                .flatMap(c -> c.getCatalogItems().stream())
                .map(TransformCatalogItemDto.class::cast)
                .map(TransformCatalogItemDto::getElement)
                .map(this::removeOwner)
                .map(IdentifiableDto.class::cast)
                .collect(Collectors.toMap(IdentifiableDto::getId, Function.identity())));
    return elementCache;
  }

  /** Fills the ref.cache and dtoCache with the elements and fix links. */
  private Map<String, Element> createElementCache(
      PlaceholderResolver ref, Map<String, IdentifiableDto> elementDtos) {
    ref.dtoCache = elementDtos;

    Map<AbstractElementDto, Map<String, List<CustomLinkDto>>> linkCache = new HashMap<>();
    Map<AbstractScopeDto, Set<IdRef<Element>>> memberCache = new HashMap<>();

    Predicate<IdentifiableDto> isScope = AbstractScopeDto.class::isInstance;

    // all not scopes
    elementDtos.values().stream()
        .filter(Predicate.not(isScope))
        .map(AbstractElementDto.class::cast)
        .map(
            e -> {
              linkCache.put(e, Map.copyOf(e.getLinks()));
              e.getLinks().clear();
              return e;
            })
        .map(e -> entityTransformer.transformDto2Element(e, ref))
        .forEach(c -> ref.cache.put(c.getIdAsString(), c));

    // all scopes
    elementDtos.values().stream()
        .filter(isScope)
        .map(AbstractScopeDto.class::cast)
        .map(
            e -> {
              linkCache.put(e, Map.copyOf(e.getLinks()));
              e.getLinks().clear();
              return e;
            })
        .map(
            e -> {
              memberCache.put(e, Set.copyOf(e.getMembers()));
              e.getMembers().clear();
              return e;
            })
        .map(e -> entityTransformer.transformDto2Scope(e, ref))
        .forEach(c -> ref.cache.put(c.getIdAsString(), c));

    linkCache.entrySet().forEach(e -> e.getKey().setLinks(e.getValue()));
    memberCache.entrySet().forEach(e -> (e.getKey()).setMembers(e.getValue()));

    elementDtos.values().stream()
        .map(AbstractElementDto.class::cast)
        .map(e -> entityTransformer.transformDto2Element(e, ref))
        .forEach(
            c -> {
              ref.cache.put(c.getIdAsString(), c);
            });

    Map<String, Element> elementCache =
        ref.cache.entrySet().stream()
            .filter(e -> e.getValue() instanceof Element && elementDtos.containsKey(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, e -> (Element) e.getValue()));

    ref.cache.entrySet().stream()
        .filter(e -> (e.getValue() instanceof Element && elementDtos.containsKey(e.getKey())))
        .map(e -> (Element) e.getValue())
        .forEach(
            es -> {
              es.getLinks()
                  .forEach(
                      link -> {
                        if (link.getTarget().getId() != null) {
                          Element element = elementCache.get(link.getTarget().getIdAsString());
                          link.setTarget(element);
                        }
                      });
              if (es instanceof CompositeElement) {
                CompositeElement<CompositeElement> ce = (CompositeElement<CompositeElement>) es;
                Set<String> partIds =
                    ce.getParts().stream()
                        .map(Identifiable::getIdAsString)
                        .collect(Collectors.toSet());
                Set<CompositeElement> resolvedParts =
                    elementCache.entrySet().stream()
                        .filter(e -> partIds.contains(e.getKey()))
                        .map(Entry::getValue)
                        .map(it -> (CompositeElement) it)
                        .collect(Collectors.toSet());
                ce.setParts(resolvedParts);
              }
              if (es instanceof Scope) {
                Scope se = (Scope) es;
                Set<String> memberIds =
                    se.getMembers().stream()
                        .map(Identifiable::getIdAsString)
                        .collect(Collectors.toSet());
                Set<Element> resolvedMembers =
                    elementCache.entrySet().stream()
                        .filter(e -> memberIds.contains(e.getKey()))
                        .map(Entry::getValue)
                        .collect(Collectors.toSet());
                se.setMembers(resolvedMembers);
              }
            });

    return elementCache;
  }

  private Object transformTailorRef(
      TransformCatalogItemDto source,
      Map<String, CatalogItem> itemCache,
      PlaceholderResolver idRefResolver) {

    CatalogItem target = itemCache.get(source.getId());
    target.getTailoringReferences().clear();
    target.setTailoringReferences(
        source.getTailoringReferences().stream()
            .map(tr -> entityTransformer.transformDto2TailoringReference(tr, target, idRefResolver))
            .collect(Collectors.toSet()));

    return target;
  }

  private CatalogItem transformCatalogItem(
      TransformCatalogItemDto source,
      Map<String, Element> elementCache,
      PlaceholderResolver idRefResolver) {
    Set<AbstractTailoringReferenceDto> tailoringReferences =
        new HashSet<>(source.getTailoringReferences());
    source.getTailoringReferences().clear();
    var target =
        entityTransformer.transformDto2CatalogItem(
            source, idRefResolver, idRefResolver.resolve(source.getCatalog()));
    source.getTailoringReferences().addAll(tailoringReferences);
    String id = ((IdentifiableDto) source.getElement()).getId();
    Element aElement = elementCache.get(id);
    target.setElement(aElement);
    if (target.getElement() != null) {
      target.getElement().setContainingCatalogItem(target);
    }
    return target;
  }

  private TransformDomainTemplateDto readInstanceFile(VeoInputStreamResource resource)
      throws IOException {
    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return objectMapper.readValue(br, TransformDomainTemplateDto.class);
    }
  }

  private <T extends AbstractElementDto> T removeOwner(T element) {
    element.setOwner(null);
    return element;
  }

  @Override
  public ExportDto exportDomain(Domain domain) {
    return dtoTransformer.transformDomain2ExportDto(domain);
  }
}
