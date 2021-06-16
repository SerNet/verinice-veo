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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractAssetDto;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.AbstractDocumentDto;
import org.veo.adapter.presenter.api.dto.AbstractIncidentDto;
import org.veo.adapter.presenter.api.dto.AbstractPersonDto;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.dto.AbstractScenarioDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.CatalogableDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.core.VeoInputStreamResource;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.service.DomainTemplateEventPublisher;
import org.veo.core.service.DomainTemplateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainTemplateServiceImpl implements DomainTemplateService {
    private static final String SYSTEM_USER = "system";
    private static final String NO_DESIGNATOR = "NO_DESIGNATOR";

    private class PlaceholderResolver extends ModelObjectReferenceResolver {
        Map<String, ModelObject> cache = new HashMap<>();
        Map<String, IdentifiableDto> dtoCache = new HashMap<>();

        private PlaceholderResolver() {
            super(null, null);
        }

        @Override
        public <TEntity extends ModelObject> TEntity resolve(
                ModelObjectReference<TEntity> objectReference) throws NotFoundException {
            if (objectReference == null) {
                return null;
            }
            String id = objectReference.getId();
            ModelObject modelObject = cache.computeIfAbsent(id,
                                                            a -> createElement(id,
                                                                               objectReference.getType()));
            return (TEntity) modelObject;
        }

        @Override
        public <TEntity extends ModelObject> Set<TEntity> resolve(
                Set<ModelObjectReference<TEntity>> objectReferences) {

            return objectReferences.stream()
                                   .map(o -> resolve(o))
                                   .collect(Collectors.toSet());
        }

        /**
         * Creates te missing element from the dto in the cache.
         */
        private ModelObject createElement(String id, Class<? extends ModelObject> type) {
            IdentifiableDto catalogableDto = dtoCache.get(id);
            if (catalogableDto != null) {
                EntityLayerSupertypeDto es = (EntityLayerSupertypeDto) catalogableDto;
                HashMap<String, List<CustomLinkDto>> hashMap = new HashMap<>(es.getLinks());
                es.getLinks()
                  .clear();
                Catalogable catalogable = transform(es, this);
                es.getLinks()
                  .putAll(hashMap);
                return catalogable;
            }
            throw new IllegalArgumentException(
                    "Unknown type (not dtoCached):" + type + "  id:" + id);
        }
    }

    private final DomainTemplateRepository domainTemplateRepository;
    private final DtoToEntityTransformer entityTransformer;
    private final EntityFactory factory;
    private final DomainTemplateEventPublisher publisher;
    private final List<VeoInputStreamResource> domainResources;

    private ReferenceAssembler assembler;
    private ObjectMapper objectMapper;
    private ReferenceDeserializer deserializer;
    private Map<String, DomainTemplate> bootstrappedDomaintemplates;
    private Map<String, VeoInputStreamResource> domainTemplateFiles = new HashMap<>();

    public DomainTemplateServiceImpl(DomainTemplateRepository domainTemplateRepository,
            DtoToEntityTransformer entityTransformer, EntityFactory factory,
            DomainTemplateEventPublisher publisher, List<VeoInputStreamResource> domainResources) {
        this.domainTemplateRepository = domainTemplateRepository;
        this.entityTransformer = entityTransformer;
        this.factory = factory;
        this.publisher = publisher;
        this.domainResources = domainResources;

        assembler = new LocalReferenceAssembler();
        deserializer = new ReferenceDeserializer(assembler);
        objectMapper = new ObjectMapper().registerModule(new SimpleModule().addDeserializer(ModelObjectReference.class,
                                                                                            deserializer))
                                         .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                    false);
    }

    @Override
    public void readTemplateFiles() {
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
                                                         .collect(Collectors.toMap(e -> e.getKey(),
                                                                                   e -> createDomainTemplate(e.getKey(),
                                                                                                             e.getValue())));

    }

    /**
     * Mainly used by the test, as we expect the state to be cleared each time the
     * test run.
     */
    void reInitalizeAvailableDomainTemplates() {
        bootstrappedDomaintemplates = null;
        publisher.domainServiceReinitialize();
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
                log.info("Domain {} created for client {}", domain, client);
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

    /**
     * Returns the default template id for a client.
     */
    private Set<String> getDefaultDomainTemplateIds(Client client) {
        return Collections.singleton(DSGVO_DOMAINTEMPLATE_UUID);
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
        PlaceholderResolver ref = new PlaceholderResolver();
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
                             c.setDomainTemplate(new SyntheticModelObjectReference<DomainTemplate>(
                                     domainTemplateDto.getId(), DomainTemplate.class, assembler));
                         });

        Map<String, Catalogable> elementCache = createElementCache(domainTemplateDto, ref);
        Map<String, CatalogItem> itemCache = createCatalogItemCache(domainTemplateDto, ref,
                                                                    elementCache);
        Domain domain = entityTransformer.transformDomainTemplateDto2Domain(domainTemplateDto, ref);

        domain.getCatalogs()
              .forEach(catalog -> {
                  catalog.setId(null);
                  catalog.setDomainTemplate(domain);
                  Set<CatalogItem> catalogItems = catalog.getCatalogItems()
                                                         .stream()
                                                         .map(ci -> itemCache.get(ci.getId()
                                                                                    .uuidValue()))
                                                         .collect(Collectors.toSet());
                  catalog.getCatalogItems()
                         .clear();
                  catalog.getCatalogItems()
                         .addAll(catalogItems);
                  catalog.getCatalogItems()
                         .forEach(item -> {
                             processCatalogItem(domain, catalog, item);
                         });
              });
        return domain;
    }

    private Map<String, CatalogItem> createCatalogItemCache(
            TransformDomainTemplateDto domainTemplateDto, PlaceholderResolver ref,
            Map<String, Catalogable> elementCache) {
        domainTemplateDto.getCatalogs()
                         .stream()
                         .map(c -> (TransformCatalogDto) c)
                         .flatMap(c -> c.getCatalogItems()
                                        .stream())
                         .map(ci -> (TransformCatalogItemDto) ci)
                         .map(ci -> transformCatalogItem(ci, elementCache, ref))
                         .forEach(c -> ref.cache.put(c.getId()
                                                      .uuidValue(),
                                                     c));

        Map<String, CatalogItem> itemCache = ref.cache.entrySet()
                                                      .stream()
                                                      .filter(e -> (e.getValue() instanceof CatalogItem))
                                                      .collect(Collectors.toMap(e -> e.getKey(),
                                                                                e -> (CatalogItem) e.getValue()));

        domainTemplateDto.getCatalogs()
                         .stream()
                         .map(c -> (TransformCatalogDto) c)
                         .flatMap(c -> c.getCatalogItems()
                                        .stream())
                         .map(ci -> (TransformCatalogItemDto) ci)
                         .forEach(ci -> transformTailorRef(ci, itemCache, ref));
        return itemCache;
    }

    /**
     * Fills the ref.cache and dtoCache with the elements and fix links.
     */
    private Map<String, Catalogable> createElementCache(
            TransformDomainTemplateDto domainTemplateDto, PlaceholderResolver ref) {
        Map<String, IdentifiableDto> catalogableDtos = domainTemplateDto.getCatalogs()
                                                                        .stream()
                                                                        .map(TransformCatalogDto.class::cast)
                                                                        .flatMap(c -> c.getCatalogItems()
                                                                                       .stream())
                                                                        .map(TransformCatalogItemDto.class::cast)
                                                                        .map(ci -> {
                                                                            ci.getElement()
                                                                              .setOwner(null);
                                                                            return ci.getElement();
                                                                        })
                                                                        .collect(Collectors.toMap(i -> ((IdentifiableDto) i).getId(),
                                                                                                  i -> (IdentifiableDto) i));

        ref.dtoCache = catalogableDtos;

        catalogableDtos.values()
                       .stream()
                       .map(e -> transform((CatalogableDto) e, ref))
                       .forEach(c -> ref.cache.put(c.getId()
                                                    .uuidValue(),
                                                   c));

        Map<String, Catalogable> elementCache = ref.cache.entrySet()
                                                         .stream()
                                                         .filter(e -> (e.getValue() instanceof Catalogable))
                                                         .collect(Collectors.toMap(e -> e.getKey(),
                                                                                   e -> (Catalogable) e.getValue()));

        ref.cache.entrySet()
                 .stream()
                 .filter(e -> (e.getValue() instanceof EntityLayerSupertype))
                 .map(e -> (EntityLayerSupertype) e.getValue())
                 .forEach(es -> {
                     es.getLinks()
                       .forEach(link -> {
                           if (link.getTarget()
                                   .getId() != null) {
                               Catalogable catalogable = elementCache.get(link.getTarget()
                                                                              .getId()
                                                                              .uuidValue());
                               link.setTarget((EntityLayerSupertype) catalogable);
                           }

                       });
                 });

        return elementCache;
    }

    private Object transformTailorRef(TransformCatalogItemDto source,
            Map<String, CatalogItem> itemCache, PlaceholderResolver modelObjectReferenceResolver) {

        CatalogItem target = itemCache.get(source.getId());
        target.getTailoringReferences()
              .clear();
        target.setTailoringReferences(source.getTailoringReferences()
                                            .stream()
                                            .map(tr -> entityTransformer.transformDto2TailoringReference(tr,
                                                                                                         target,
                                                                                                         modelObjectReferenceResolver))
                                            .collect(Collectors.toSet()));

        return target;
    }

    private CatalogItem transformCatalogItem(TransformCatalogItemDto source,
            Map<String, Catalogable> elementCache,
            PlaceholderResolver modelObjectReferenceResolver) {
        Set<AbstractTailoringReferenceDto> tailoringReferences = new HashSet<>(
                source.getTailoringReferences());
        source.getTailoringReferences()
              .clear();
        var target = entityTransformer.transformDto2CatalogItem(source,
                                                                modelObjectReferenceResolver,
                                                                modelObjectReferenceResolver.resolve(source.getCatalog()));
        source.getTailoringReferences()
              .addAll(tailoringReferences);
        String id = ((IdentifiableDto) source.getElement()).getId();
        Catalogable aCatalogable = elementCache.get(id);
        target.setElement(aCatalogable);
        if (target.getElement() != null) {
            target.getElement()
                  .setOwner(target);
        }
        return target;
    }

    private Catalogable transform(CatalogableDto catalogableDto,
            PlaceholderResolver modelObjectReferenceResolver) {

        if (catalogableDto instanceof AbstractAssetDto) {
            return entityTransformer.transformDto2Asset((AbstractAssetDto) catalogableDto,
                                                        modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractControlDto) {
            return entityTransformer.transformDto2Control((AbstractControlDto) catalogableDto,
                                                          modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractDocumentDto) {
            return entityTransformer.transformDto2Document((AbstractDocumentDto) catalogableDto,
                                                           modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractIncidentDto) {
            return entityTransformer.transformDto2Incident((AbstractIncidentDto) catalogableDto,
                                                           modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractPersonDto) {
            return entityTransformer.transformDto2Person((AbstractPersonDto) catalogableDto,
                                                         modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractProcessDto) {
            return entityTransformer.transformDto2Process((AbstractProcessDto) catalogableDto,
                                                          modelObjectReferenceResolver);
        } else if (catalogableDto instanceof AbstractScenarioDto) {
            return entityTransformer.transformDto2Scenario((AbstractScenarioDto) catalogableDto,
                                                           modelObjectReferenceResolver);
        }
        return null;
    }

    /**
     * Clean up and relink a catalogItem. Add the domain to each sub element.
     */
    private void processCatalogItem(Domain domain, Catalog catalog, CatalogItem item) {
        item.setId(null);
        item.setCatalog(catalog);
        Catalogable element = item.getElement();
        if (element != null) {
            processElement(domain, item, element);
        }
    }

    /**
     * Clean up and relink a catalogable. Add the domain to each sub element.
     */
    private void processElement(Domain domain, CatalogItem item, Catalogable element) {
        element.setId(null);
        if (element instanceof EntityLayerSupertype) {
            EntityLayerSupertype est = (EntityLayerSupertype) element;
            est.setDesignator(NO_DESIGNATOR);
            est.getDomains()
               .clear();
            est.addToDomains(domain);
            processSubTypes(domain, est);
            processLinks(domain, est);
            processCustomAspects(domain, est);
            // TODO: VEO-612 add parts from CompositeEntity
        } else {
            throw new IllegalArgumentException(
                    "Element not of known type: " + element.getModelInterface()
                                                           .getSimpleName());
        }
    }

    private void processCustomAspects(Domain domain, EntityLayerSupertype est) {
        est.getCustomAspects()
           .forEach(ca -> {
               ca.getDomains()
                 .clear();
               ca.addToDomains(domain);
           });
    }

    private void processLinks(Domain domain, EntityLayerSupertype est) {
        est.getLinks()
           .forEach(link -> {
               link.getDomains()
                   .clear();
               link.addToDomains(domain);
           });
    }

    private void processSubTypes(Domain domain, EntityLayerSupertype est) {
        if (!est.getSubTypeAspects()
                .isEmpty()) {
            List<String> aspects = est.getSubTypeAspects()
                                      .stream()
                                      .map(sa -> sa.getSubType())
                                      .collect(Collectors.toList());
            est.getSubTypeAspects()
               .clear();
            aspects.forEach(a -> est.setSubType(domain, a));
        }
    }

    private TransformDomainTemplateDto readInstanceFile(VeoInputStreamResource resource)
            throws JsonParseException, JsonMappingException, IOException {

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            TransformDomainTemplateDto domainTemplateDto = objectMapper.readValue(br,
                                                                                  TransformDomainTemplateDto.class);
            return domainTemplateDto;
        }
    }

}
