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
import java.time.Instant;
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
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformElementDto;
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
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainTemplateServiceImpl implements DomainTemplateService {
    private static final String SYSTEM_USER = "system";

    private final DomainTemplateRepository domainTemplateRepository;
    private final DtoToEntityTransformer entityTransformer;
    private final EntityFactory factory;
    private final List<VeoInputStreamResource> domainResources;
    private final CatalogItemPrepareStrategy preparations;
    private final Set<String> defaultDomainTemplateIds;

    private ReferenceAssembler assembler;
    private ObjectMapper objectMapper;
    private ReferenceDeserializer deserializer;
    private Map<String, DomainTemplate> bootstrappedDomaintemplates;
    private Map<String, VeoInputStreamResource> domainTemplateFiles = new HashMap<>();

    public DomainTemplateServiceImpl(DomainTemplateRepository domainTemplateRepository,
            EntityFactory factory, List<VeoInputStreamResource> domainResources,
            DomainAssociationTransformer domainAssociationTransformer,
            CatalogItemPrepareStrategy preparations, Set<String> defaultDomainTemplateIds) {
        this.domainTemplateRepository = domainTemplateRepository;
        this.factory = factory;
        this.domainResources = domainResources;
        this.preparations = preparations;
        this.defaultDomainTemplateIds = defaultDomainTemplateIds;

        entityTransformer = new DtoToEntityTransformer(factory, domainAssociationTransformer);
        assembler = new LocalReferenceAssembler();
        deserializer = new ReferenceDeserializer(assembler);
        objectMapper = new ObjectMapper().addMixIn(AbstractElementDto.class,
                                                   TransformElementDto.class)
                                         .registerModule(new SimpleModule().addDeserializer(IdRef.class,
                                                                                            deserializer))
                                         .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                    false);
        readTemplateFiles();
    }

    private void readTemplateFiles() {
        log.info("read files from resources ...");
        domainResources.forEach(f -> {
            log.info("read file:{}", f.getDescription());
            TransformDomainTemplateDto templateDto = null;
            try {
                templateDto = readInstanceFile(f);
                domainTemplateFiles.put(templateDto.getId(), f);
            } catch (IOException e) {
                log.error("Error reading file", e);
            }
        });

        bootstrappedDomaintemplates = domainTemplateFiles.entrySet()
                                                         .stream()
                                                         .collect(Collectors.toMap(Entry::getKey,
                                                                                   e -> createDomainTemplate(e.getKey(),
                                                                                                             e.getValue())));

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
    public Set<Domain> createDefaultDomains(Client client) {
        if (!client.getDomains()
                   .isEmpty()) {
            throw new IllegalArgumentException("The client owns already domains.");
        }
        Set<String> templateIds = getDefaultDomainTemplateIds(client);
        log.info("Construct new default domains from templates [{}] for Client {}", templateIds,
                 client);
        return templateIds.stream()
                          .map(id -> createDomain(client, id))
                          .collect(Collectors.toSet());
    }

    @Override
    public Domain createDomain(Client client, String templateId) {
        VeoInputStreamResource templateFile = domainTemplateFiles.get(templateId);
        if (templateFile != null) {
            // TODO: check the rights and throw another exception, or another
            // error text
            // like :
            // please purchase the domaintemplate in the shop
            try {
                TransformDomainTemplateDto domainTemplateDto = readInstanceFile(templateFile);
                Domain domain = processDomainTemplate(domainTemplateDto);
                domain.setDomainTemplate(bootstrappedDomaintemplates.get(templateId));
                client.addToDomains(domain);
                log.info("Domain {} created for client {}", domain.getName(), client);
                return domain;
            } catch (JsonMappingException e) {
                log.error("Error parsing file", e);
            } catch (IOException e) {
                log.error("Error loading file", e);
            }
        }
        throw new NotFoundException("Domain template %s does not exist for client %s.", templateId,
                client);
    }

    @Override
    public Collection<Element> getElementsForDemoUnit(Client client) {
        log.info("Create demo unit elements for {}", client);
        Set<Element> elements = new HashSet<>();
        PlaceholderResolver ref = new PlaceholderResolver(entityTransformer);
        client.getDomains()
              .forEach(domain -> {
                  log.info("Processing {}", domain);
                  VeoInputStreamResource templateFile = domainTemplateFiles.get(domain.getDomainTemplate()
                                                                                      .getId()
                                                                                      .uuidValue());
                  try {
                      TransformDomainTemplateDto domainTemplateDto = readInstanceFile(templateFile);
                      ref.cache.put(domainTemplateDto.getId(), domain);
                      Map<String, Element> elementCache = createElementCache(ref,
                                                                             domainTemplateDto.getDemoUnitElements()
                                                                                              .stream()
                                                                                              .map(this::removeOwner)
                                                                                              .map(IdentifiableDto.class::cast)
                                                                                              .collect(Collectors.toMap(IdentifiableDto::getId,
                                                                                                                        Function.identity())));

                      elementCache.entrySet()
                                  .forEach(e -> {
                                      Element value = e.getValue();
                                      value.setDomains(Collections.singleton(domain));
                                      value.setId(null);
                                      value.getSubTypeAspects()
                                           .stream()
                                           .forEach(st -> st.setDomain(domain));
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
     * Returns the default template id for a client.
     */
    private Set<String> getDefaultDomainTemplateIds(Client client) {
        return defaultDomainTemplateIds;
    }

    /**
     * This will bootstrap the data in the db, the domain template is created and
     * inserted in or returned from the database. We currently store the basic data.
     */
    public DomainTemplate createDomainTemplate(String id, VeoInputStreamResource templateFile) {
        Optional<DomainTemplate> dt = domainTemplateRepository.findById(Key.uuidFrom(id));
        if (dt.isPresent()) {
            return dt.get();
        }
        try {
            TransformDomainTemplateDto domainTemplateDto = readInstanceFile(templateFile);
            DomainTemplate domainTemplate = factory.createDomainTemplate(domainTemplateDto.getName(),
                                                                         domainTemplateDto.getAuthority(),
                                                                         domainTemplateDto.getTemplateVersion(),
                                                                         domainTemplateDto.getRevision(),
                                                                         Key.uuidFrom(id));

            domainTemplate.setDescription(domainTemplateDto.getDescription());
            domainTemplate.setAbbreviation(domainTemplateDto.getAbbreviation());
            domainTemplate.setCreatedBy(SYSTEM_USER);
            domainTemplate.setUpdatedBy(SYSTEM_USER);
            domainTemplate.setCreatedAt(Instant.now());
            log.info("Create and save domain template {}", domainTemplate);
            domainTemplate = domainTemplateRepository.save(domainTemplate);
            return domainTemplate;
        } catch (JsonMappingException e) {
            log.error("Error parsing file", e);
        } catch (IOException e) {
            log.error("Error loading file", e);
        }
        throw new NotFoundException("Domain template %s file %s not found.", id, templateFile);
    }

    /**
     * Transform the given domainTemplateDto to a new domain.
     */
    private Domain processDomainTemplate(TransformDomainTemplateDto domainTemplateDto) {
        PlaceholderResolver ref = new PlaceholderResolver(entityTransformer);
        Domain domainPlaceholder = factory.createDomain(domainTemplateDto.getName(),
                                                        domainTemplateDto.getAuthority(),
                                                        domainTemplateDto.getTemplateVersion(),
                                                        domainTemplateDto.getRevision());
        domainPlaceholder.setDescription(domainTemplateDto.getDescription());
        domainPlaceholder.setAbbreviation(domainTemplateDto.getAbbreviation());

        domainPlaceholder.setId(Key.uuidFrom(domainTemplateDto.getId()));
        ref.cache.put(domainTemplateDto.getId(), domainPlaceholder);

        domainTemplateDto.getCatalogs()
                         .stream()
                         .forEach(c -> {
                             Catalog createCatalog = factory.createCatalog(domainPlaceholder);
                             ref.cache.put(((IdentifiableDto) c).getId(), createCatalog);
                             c.setDomainTemplate(new SyntheticIdRef<>(domainTemplateDto.getId(),
                                     DomainTemplate.class, assembler));
                         });

        Map<String, Element> elementCache = createElementCache(ref, domainTemplateDto.getCatalogs()
                                                                                     .stream()
                                                                                     .map(TransformCatalogDto.class::cast)
                                                                                     .flatMap(c -> c.getCatalogItems()
                                                                                                    .stream())
                                                                                     .map(TransformCatalogItemDto.class::cast)
                                                                                     .map(TransformCatalogItemDto::getElement)
                                                                                     .map(this::removeOwner)
                                                                                     .map(IdentifiableDto.class::cast)
                                                                                     .collect(Collectors.toMap(IdentifiableDto::getId,
                                                                                                               Function.identity())));

        Map<String, CatalogItem> itemCache = createCatalogItemCache(domainTemplateDto, ref,
                                                                    elementCache);
        Domain domain = entityTransformer.transformTransformDomainTemplateDto2Domain(domainTemplateDto,
                                                                                     ref);

        domain.getCatalogs()
              .forEach(catalog -> {
                  catalog.setId(null);
                  catalog.setDomainTemplate(domain);
                  Set<CatalogItem> catalogItems = catalog.getCatalogItems()
                                                         .stream()
                                                         .map(ci -> itemCache.get(ci.getIdAsString()))
                                                         .collect(Collectors.toSet());
                  catalog.getCatalogItems()
                         .clear();
                  catalog.getCatalogItems()
                         .addAll(catalogItems);
                  catalog.getCatalogItems()
                         .forEach(item -> preparations.prepareCatalogItem(domain, catalog, item));
              });
        return domain;
    }

    private Map<String, CatalogItem> createCatalogItemCache(
            TransformDomainTemplateDto domainTemplateDto, PlaceholderResolver ref,
            Map<String, Element> elementCache) {
        domainTemplateDto.getCatalogs()
                         .stream()
                         .map(TransformCatalogDto.class::cast)
                         .flatMap(c -> c.getCatalogItems()
                                        .stream())
                         .map(TransformCatalogItemDto.class::cast)
                         .map(ci -> transformCatalogItem(ci, elementCache, ref))
                         .forEach(c -> ref.cache.put(c.getIdAsString(), c));

        Map<String, CatalogItem> itemCache = ref.cache.entrySet()
                                                      .stream()
                                                      .filter(e -> (e.getValue() instanceof CatalogItem))
                                                      .collect(Collectors.toMap(Entry::getKey,
                                                                                e -> (CatalogItem) e.getValue()));

        domainTemplateDto.getCatalogs()
                         .stream()
                         .map(TransformCatalogDto.class::cast)
                         .flatMap(c -> c.getCatalogItems()
                                        .stream())
                         .map(TransformCatalogItemDto.class::cast)
                         .forEach(ci -> transformTailorRef(ci, itemCache, ref));
        return itemCache;
    }

    /**
     * Fills the ref.cache and dtoCache with the elements and fix links.
     */
    private Map<String, Element> createElementCache(PlaceholderResolver ref,
            Map<String, IdentifiableDto> elementDtos) {
        ref.dtoCache = elementDtos;

        Map<AbstractElementDto, Map<String, List<CustomLinkDto>>> linkCache = new HashMap<>();

        Predicate<IdentifiableDto> isScope = AbstractScopeDto.class::isInstance;

        // all not scopes
        elementDtos.values()
                   .stream()
                   .filter(Predicate.not(isScope))
                   .map(AbstractElementDto.class::cast)
                   .map(e -> {
                       linkCache.put(e, e.getLinks());
                       e.getLinks()
                        .clear();
                       return e;
                   })
                   .map(e -> entityTransformer.transformDto2Element(e, ref))
                   .forEach(c -> ref.cache.put(c.getIdAsString(), c));

        // all scopes
        elementDtos.values()
                   .stream()
                   .filter(isScope)
                   .map(AbstractScopeDto.class::cast)
                   .map(e -> {
                       linkCache.put(e, e.getLinks());
                       e.getLinks()
                        .clear();
                       return e;
                   })
                   .map(e -> entityTransformer.transformDto2Scope(e, ref))
                   .forEach(c -> ref.cache.put(c.getIdAsString(), c));

        linkCache.entrySet()
                 .forEach(e -> e.getKey()
                                .setLinks(e.getValue()));

        Map<String, Element> elementCache = ref.cache.entrySet()
                                                     .stream()
                                                     .filter(e -> e.getValue() instanceof Element
                                                             && elementDtos.containsKey(e.getKey()))
                                                     .collect(Collectors.toMap(Entry::getKey,
                                                                               e -> (Element) e.getValue()));

        ref.cache.entrySet()
                 .stream()
                 .filter(e -> (e.getValue() instanceof Element))
                 .map(e -> (Element) e.getValue())
                 .forEach(es -> {
                     es.getLinks()
                       .forEach(link -> {
                           if (link.getTarget()
                                   .getId() != null) {
                               Element element = elementCache.get(link.getTarget()
                                                                      .getIdAsString());
                               link.setTarget(element);
                           }

                       });
                     if (es instanceof CompositeElement) {
                         CompositeElement<Element> ce = (CompositeElement<Element>) es;
                         Set<String> partIds = ce.getParts()
                                                 .stream()
                                                 .map(Identifiable::getIdAsString)
                                                 .collect(Collectors.toSet());
                         Set<Element> resolvedParts = elementCache.entrySet()
                                                                  .stream()
                                                                  .filter(e -> partIds.contains(e.getKey()))
                                                                  .map(Entry::getValue)
                                                                  .collect(Collectors.toSet());
                         ce.setParts(resolvedParts);
                     }
                 });

        return elementCache;
    }

    private Object transformTailorRef(TransformCatalogItemDto source,
            Map<String, CatalogItem> itemCache, PlaceholderResolver idRefResolver) {

        CatalogItem target = itemCache.get(source.getId());
        target.getTailoringReferences()
              .clear();
        target.setTailoringReferences(source.getTailoringReferences()
                                            .stream()
                                            .map(tr -> entityTransformer.transformDto2TailoringReference(tr,
                                                                                                         target,
                                                                                                         idRefResolver))
                                            .collect(Collectors.toSet()));

        return target;
    }

    private CatalogItem transformCatalogItem(TransformCatalogItemDto source,
            Map<String, Element> elementCache, PlaceholderResolver idRefResolver) {
        Set<AbstractTailoringReferenceDto> tailoringReferences = new HashSet<>(
                source.getTailoringReferences());
        source.getTailoringReferences()
              .clear();
        var target = entityTransformer.transformDto2CatalogItem(source, idRefResolver,
                                                                idRefResolver.resolve(source.getCatalog()));
        source.getTailoringReferences()
              .addAll(tailoringReferences);
        String id = ((IdentifiableDto) source.getElement()).getId();
        Element aElement = elementCache.get(id);
        target.setElement(aElement);
        if (target.getElement() != null) {
            target.getElement()
                  .setContainingCatalogItem(target);
        }
        return target;
    }

    private TransformDomainTemplateDto readInstanceFile(VeoInputStreamResource resource)
            throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return objectMapper.readValue(br, TransformDomainTemplateDto.class);
        }
    }

    private <T extends AbstractElementDto> T removeOwner(T element) {
        element.setOwner(null);
        return element;
    }
}