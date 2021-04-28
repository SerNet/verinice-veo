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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.CatalogableDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReferenceType;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple domaintemplate builder.
 */
@Slf4j
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

        objectMapper = new ObjectMapper().addMixIn(CatalogableDto.class,
                                                   TransformCatalogableDto.class)
                                         .registerModule(new SimpleModule().addDeserializer(ModelObjectReference.class,
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
     * tom.catalog.name="DVGO-Controls"<br/>
     * tom.dir="/verinice-veo/domaintemplates/dsgvo/tom"<br/>
     * tom.prefix="TOM."<br/>
     */
    // TODO: VEO-636 add as gradle task to build step
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
    public TransformDomainTemplateDto createDomainTemplateDto() {
        TransformDomainTemplateDto domainTemplateDto = new TransformDomainTemplateDto();
        domainTemplateDto.setId(id);

        catalogs.forEach(c -> c.setDomainTemplate(new SyntheticModelObjectReference<DomainTemplate>(
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
     * Create a catalog from a set of Catalogables which are supplied as a resources
     * array. The catalog is stored in the assembler and will be added to the domain
     * when {@link #createDomainTemplateDto()} is called.
     */
    public void createCatalog(File[] resources, String catalogName,
            Function<EntityLayerSupertypeDto, String> toNamespace)
            throws JsonParseException, JsonMappingException, IOException {
        String catalogId = Key.newUuid()
                              .uuidValue();
        Map<String, CatalogableDto> readElements = readElements(resources);
        Map<String, TransformCatalogItemDto> catalogItems = createCatalogItems(readElements,
                                                                               catalogId,
                                                                               toNamespace);

        for (Entry<String, CatalogableDto> e : readElements.entrySet()) {
            createTailoringReferences(e.getValue(), catalogItems);
        }
        TransformCatalogDto catalogDto = new TransformCatalogDto();
        catalogDto.setName(catalogName);
        catalogDto.setId(catalogId);
        catalogDto.getCatalogItems()
                  .addAll(catalogItems.values());

        catalogs.add(catalogDto);
    }

    private Map<String, TransformCatalogItemDto> createCatalogItems(
            Map<String, CatalogableDto> readElements, String catalogId,
            Function<EntityLayerSupertypeDto, String> toNamespace) {
        Map<String, TransformCatalogItemDto> cache = new HashMap<>();
        for (Entry<String, CatalogableDto> e : readElements.entrySet()) {
            TransformCatalogItemDto itemDto = new TransformCatalogItemDto();
            itemDto.setElement(e.getValue());
            itemDto.setCatalog(SyntheticModelObjectReference.from(catalogId, Catalog.class));
            EntityLayerSupertypeDto supertypeDto = (EntityLayerSupertypeDto) e.getValue();
            itemDto.setNamespace(toNamespace.apply(supertypeDto));
            itemDto.setId(Key.newUuid()
                             .uuidValue());
            supertypeDto.setOwner(SyntheticModelObjectReference.from(itemDto.getId(),
                                                                     ElementOwner.class,
                                                                     CatalogItem.class));
            supertypeDto.setType(SyntheticModelObjectReference.toSingularTerm(supertypeDto.getModelInterface()));
            cache.put(e.getKey(), itemDto);
        }
        return cache;
    }

    private void createTailoringReferences(CatalogableDto value,
            Map<String, TransformCatalogItemDto> catalogItems) {
        if (value instanceof EntityLayerSupertypeDto) {
            EntityLayerSupertypeDto els = (EntityLayerSupertypeDto) value;
            TransformCatalogItemDto currentItem = catalogItems.get(((IdentifiableDto) value).getId());
            currentItem.setTailoringReferences(new HashSet<AbstractTailoringReferenceDto>());
            els.getLinks()
               .entrySet()
               .stream()
               .flatMap(e -> e.getValue()
                              .stream())
               .forEach(l -> {
                   TransformCatalogItemDto itemDto = catalogItems.get(l.getTarget()
                                                                       .getId());
                   CreateTailoringReferenceDto referenceDto = new CreateTailoringReferenceDto();
                   referenceDto.setCatalogItem(new SyntheticModelObjectReference<CatalogItem>(
                           itemDto.getId(), CatalogItem.class, assembler));
                   referenceDto.setReferenceType(TailoringReferenceType.LINK);
                   currentItem.getTailoringReferences()
                              .add(referenceDto);
               });
            CompositeEntityDto<?> e = (CompositeEntityDto<?>) els;
            e.getParts()
             .forEach(p -> {
                 TransformCatalogItemDto itemDto = catalogItems.get(p.getId());
                 CreateTailoringReferenceDto referenceDto = new CreateTailoringReferenceDto();
                 referenceDto.setCatalogItem(new SyntheticModelObjectReference<CatalogItem>(
                         itemDto.getId(), CatalogItem.class, assembler));
                 currentItem.getTailoringReferences()
                            .add(referenceDto);
                 referenceDto.setReferenceType(TailoringReferenceType.COPY);
             });
        }
    }

    private Map<String, CatalogableDto> readElements(File[] resources)
            throws JsonParseException, JsonMappingException, IOException {
        Map<String, CatalogableDto> cache = new HashMap<>();
        for (File resource : resources) {
            CatalogableDto catalogableDto = readInstanceFile(resource);
            if (catalogableDto instanceof IdentifiableDto) {
                IdentifiableDto idto = (IdentifiableDto) catalogableDto;
                cache.put(idto.getId(), catalogableDto);
            }
        }
        return cache;
    }

    private CatalogableDto readInstanceFile(File resource)
            throws JsonParseException, JsonMappingException, IOException {
        try (BufferedReader br = Files.newBufferedReader(resource.toPath(),
                                                         StandardCharsets.UTF_8)) {
            CatalogableDto domainTemplateDto = objectMapper.readValue(br, CatalogableDto.class);
            return domainTemplateDto;
        }
    }
}
