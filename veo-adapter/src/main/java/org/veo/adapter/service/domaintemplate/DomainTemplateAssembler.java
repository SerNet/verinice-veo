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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.CustomTypedLinkDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformElementDto;
import org.veo.adapter.service.domaintemplate.dto.TransformExternalTailoringReference;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReferenceType;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple domaintemplate builder. This is used by the assembleDomainTemplates
 * Gradle task and should not be called from anywhere else. This is a temporary
 * solution until VEO-399 is implemented.
 */
@Slf4j
@Deprecated
// TODO VEO-399 remove this class
public class DomainTemplateAssembler {
    private final ReferenceAssembler assembler;

    private String id;
    private String name;
    private String abbreviation;
    private String description;
    private String authority;
    private String templateVersion;
    private String revision;

    private Set<AbstractCatalogDto> catalogs = new HashSet<>();
    private ObjectMapper objectMapper;

    public DomainTemplateAssembler(ReferenceAssembler assembler,
            ReferenceDeserializer deserializer) {
        this.assembler = assembler;

        objectMapper = new ObjectMapper().addMixIn(AbstractElementDto.class,
                                                   TransformElementDto.class)
                                         .registerModule(new SimpleModule().addDeserializer(IdRef.class,
                                                                                            deserializer))
                                         .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                    false)
                                         .setSerializationInclusion(Include.NON_NULL);

    }

    /**
     * Uses environment variables:<br/>
     * domaintemplate.abbreviation="DSGVO"<br/>
     * domaintemplate.authority="SERNET"<br/>
     * domaintemplate.description="The DSGVO DomainTemplate."<br/>
     * domaintemplate.id="f8ed22b1-b277-56ec-a2ce-0dbd94e24824"<br/>
     * domaintemplate.name="DSGVO"<br/>
     * domaintemplate.out.file="dsgvo-example.json"<br/>
     * domaintemplate.revision="latest"<br/>
     * domaintemplate.templateVersion="1.0"<br/>
     * tom.catalog.name="DSGVO-Controls"<br/>
     * tom.dir="/verinice-veo/domaintemplates/dsgvo/tom"<br/>
     * tom.prefix="TOM."<br/>
     */
    public static void main(String[] args) {
        ReferenceAssembler urlAssembler = new LocalReferenceAssembler();
        ReferenceDeserializer deserializer = new ReferenceDeserializer(urlAssembler);
        DomainTemplateAssembler assembler = new DomainTemplateAssembler(urlAssembler, deserializer);

        assembler.id = System.getenv("domaintemplate.id");
        assembler.name = System.getenv("domaintemplate.name");
        assembler.abbreviation = System.getenv("domaintemplate.abbreviation");
        assembler.description = System.getenv("domaintemplate.description");
        assembler.authority = System.getenv("domaintemplate.authority");
        assembler.revision = System.getenv("domaintemplate.revision");
        assembler.templateVersion = System.getenv("domaintemplate.templateVersion");

        String outFile = System.getenv("domaintemplate.out.file");

        try {
            addCatalog(assembler, "tom");

            TransformDomainTemplateDto templateDto = assembler.createDomainTemplateDto();
            assembler.objectMapper.writerFor(TransformDomainTemplateDto.class)
                                  .writeValue(new File(outFile), templateDto);
        } catch (IOException e) {
            log.error("Error writing domain", e);
            System.exit(1);
        }
    }

    /**
     * Uses catalogPrefix +".catalog.name", catalogPrefix +".prefix", catalogPrefix
     * +".dir" as environment variable. Expect all to be set to create a catalog.
     */
    private static void addCatalog(DomainTemplateAssembler assembler, String catalogPrefix)
            throws JsonParseException, JsonMappingException, IOException {
        String catalogName = System.getenv(catalogPrefix + ".catalog.name");
        String elementDir = System.getenv(catalogPrefix + ".dir");
        String elementPrefix = System.getenv(catalogPrefix + ".prefix");
        if (catalogName != null && elementDir != null && elementPrefix != null) {
            File file = new File(elementDir);
            File[] listFiles = file.listFiles(f -> f.getName()
                                                    .endsWith(".json"));
            assembler.createCatalog(listFiles, catalogName,
                                    e -> elementPrefix + e.getAbbreviation());
        }
    }

    /**
     * Creates a domain template with all previous thru
     * {@link #createCatalog(Resource[], String)} created catalogs.
     */
    private TransformDomainTemplateDto createDomainTemplateDto() {
        TransformDomainTemplateDto domainTemplateDto = new TransformDomainTemplateDto();
        domainTemplateDto.setId(id);

        catalogs.forEach(c -> c.setDomainTemplate(new SyntheticIdRef<DomainTemplate>(
                domainTemplateDto.getId(), DomainTemplate.class, assembler)));
        domainTemplateDto.setCatalogs(catalogs);

        domainTemplateDto.setAbbreviation(abbreviation);
        domainTemplateDto.setName(name);
        domainTemplateDto.setDescription(description);
        domainTemplateDto.setAuthority(authority);
        domainTemplateDto.setRevision(revision);
        domainTemplateDto.setTemplateVersion(templateVersion);
        return domainTemplateDto;
    }

    /**
     * Create a catalog from a set of Elements which are supplied as a resources
     * array. The catalog is stored in the assembler and will be added to the domain
     * when {@link #createDomainTemplateDto()} is called.
     */
    private void createCatalog(File[] resources, String catalogName,
            Function<AbstractElementDto, String> toNamespace)
            throws JsonParseException, JsonMappingException, IOException {
        String catalogId = Key.newUuid()
                              .uuidValue();
        Map<String, AbstractElementDto> elementsById = readElements(resources);
        Map<String, TransformCatalogItemDto> catalogItemsById = createCatalogItems(elementsById,
                                                                                   catalogId,
                                                                                   toNamespace);

        for (Entry<String, AbstractElementDto> e : elementsById.entrySet()) {
            createTailoringReferences(e.getValue(), catalogItemsById);
            createExternalTailoringReferences(e.getKey(), catalogItemsById);
        }
        TransformCatalogDto catalogDto = new TransformCatalogDto();
        catalogDto.setName(catalogName);
        catalogDto.setId(catalogId);
        catalogDto.getCatalogItems()
                  .addAll(catalogItemsById.values());

        catalogs.add(catalogDto);
    }

    private Map<String, TransformCatalogItemDto> createCatalogItems(
            Map<String, AbstractElementDto> readElements, String catalogId,
            Function<AbstractElementDto, String> toNamespace) {
        Map<String, TransformCatalogItemDto> cache = new HashMap<>();
        for (Entry<String, AbstractElementDto> e : readElements.entrySet()) {
            TransformCatalogItemDto itemDto = new TransformCatalogItemDto();
            itemDto.setTailoringReferences(new HashSet<AbstractTailoringReferenceDto>());
            itemDto.setElement(e.getValue());
            itemDto.setCatalog(SyntheticIdRef.from(catalogId, Catalog.class));
            AbstractElementDto elementDto = e.getValue();
            itemDto.setNamespace(toNamespace.apply(elementDto));
            itemDto.setId(Key.newUuid()
                             .uuidValue());
            elementDto.setOwner(SyntheticIdRef.from(itemDto.getId(), ElementOwner.class,
                                                    CatalogItem.class));
            elementDto.setType(SyntheticIdRef.toSingularTerm(elementDto.getModelInterface()));
            elementDto.getDomains()
                      .add(SyntheticIdRef.from(id, Domain.class, Domain.class));
            cache.put(e.getKey(), itemDto);
        }
        return cache;
    }

    /**
     * Creates the opposite feature for the element defined by targetId. So for
     * every link in an element we create an externalTairoRef in the target of the
     * link with the link data. Except for it self.
     */
    private void createExternalTailoringReferences(String targetId,
            Map<String, TransformCatalogItemDto> catalogItems) {
        TransformCatalogItemDto targetItem = catalogItems.get(targetId);
        catalogItems.entrySet()
                    .stream()
                    .filter(entries -> !entries.getKey()
                                               .equals(targetId))// exclude self
                    .map(entries -> entries.getValue())
                    .forEach(catalogItemDto -> {
                        AbstractElementDto element = catalogItemDto.getElement();
                        for (Entry<String, List<CustomLinkDto>> typedLinks : element.getLinks()
                                                                                    .entrySet()) {
                            Stream<CustomLinkDto> allLinksToTarget = typedLinks.getValue()
                                                                               .stream()
                                                                               .filter(link -> link.getTarget()
                                                                                                   .getId()
                                                                                                   .equals(targetId));
                            allLinksToTarget.forEach(link -> {
                                CustomTypedLinkDto linkData = new CustomTypedLinkDto();
                                linkData.setTarget(new SyntheticIdRef<>(targetId, link.getTarget()
                                                                                      .getType(),
                                        assembler));
                                linkData.setAttributes(new HashMap<>(link.getAttributes()));
                                linkData.setType(typedLinks.getKey());

                                TransformExternalTailoringReference referenceDto = new TransformExternalTailoringReference();
                                referenceDto.setCatalogItem(new SyntheticIdRef<CatalogItem>(
                                        targetItem.getId(), CatalogItem.class, assembler));
                                referenceDto.setReferenceType(TailoringReferenceType.LINK_EXTERNAL);
                                referenceDto.setExternalLink(linkData);
                                targetItem.getTailoringReferences()
                                          .add(referenceDto);
                            });
                        }
                    });
    }

    private void createTailoringReferences(AbstractElementDto value,
            Map<String, TransformCatalogItemDto> catalogItems) {
        TransformCatalogItemDto currentItem = catalogItems.get(((IdentifiableDto) value).getId());
        currentItem.setTailoringReferences(new HashSet<AbstractTailoringReferenceDto>());
        value.getLinks()
             .entrySet()
             .stream()
             .flatMap(e -> e.getValue()
                            .stream())
             .forEach(l -> {
                 TransformCatalogItemDto itemDto = catalogItems.get(l.getTarget()
                                                                     .getId());
                 CreateTailoringReferenceDto referenceDto = new CreateTailoringReferenceDto();
                 referenceDto.setCatalogItem(new SyntheticIdRef<CatalogItem>(itemDto.getId(),
                         CatalogItem.class, assembler));
                 referenceDto.setReferenceType(TailoringReferenceType.LINK);
                 currentItem.getTailoringReferences()
                            .add(referenceDto);
             });
        CompositeEntityDto<?> e = (CompositeEntityDto<?>) value;
        e.getParts()
         .forEach(p -> {
             TransformCatalogItemDto itemDto = catalogItems.get(p.getId());
             CreateTailoringReferenceDto referenceDto = new CreateTailoringReferenceDto();
             referenceDto.setCatalogItem(new SyntheticIdRef<CatalogItem>(itemDto.getId(),
                     CatalogItem.class, assembler));
             currentItem.getTailoringReferences()
                        .add(referenceDto);
             referenceDto.setReferenceType(TailoringReferenceType.COPY);
         });
    }

    private Map<String, AbstractElementDto> readElements(File[] resources)
            throws JsonParseException, JsonMappingException, IOException {
        Map<String, AbstractElementDto> cache = new HashMap<>();
        for (File resource : resources) {
            AbstractElementDto elementDto = readInstanceFile(resource);
            if (elementDto instanceof IdentifiableDto) {
                IdentifiableDto idto = (IdentifiableDto) elementDto;
                cache.put(idto.getId(), elementDto);
            }
        }
        return cache;
    }

    private AbstractElementDto readInstanceFile(File resource)
            throws JsonParseException, JsonMappingException, IOException {
        log.info("process file: {}", resource);
        try (BufferedReader br = Files.newBufferedReader(resource.toPath(),
                                                         StandardCharsets.UTF_8)) {
            AbstractElementDto domainTemplateDto = objectMapper.readValue(br,
                                                                          AbstractElementDto.class);
            return domainTemplateDto;
        }
    }
}
