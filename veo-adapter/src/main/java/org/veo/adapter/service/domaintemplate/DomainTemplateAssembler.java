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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.create.CreateTailoringReferenceDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogDto;
import org.veo.adapter.service.domaintemplate.dto.TransformCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformLinkTailoringReference;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReferenceType;

import lombok.RequiredArgsConstructor;

/**
 * Takes domain template information and creates a
 * {@link TransformDomainTemplateDto} from it. Catalogs can be added by passing
 * element DTOs from which catalog items will be created.
 */
@RequiredArgsConstructor
class DomainTemplateAssembler {

    private final ReferenceAssembler assembler;

    private final String id;
    private final String name;
    private final String abbreviation;
    private final String description;
    private final String authority;
    private final String templateVersion;
    private final String revision;
    private final Set<AbstractCatalogDto> catalogs = new HashSet<>();
    private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();

    /**
     * Creates a domain template with all catalogs previously added by
     * {@link #addCatalog(String, String, Map)}.
     */
    public TransformDomainTemplateDto createDomainTemplateDto() {
        TransformDomainTemplateDto domainTemplateDto = new TransformDomainTemplateDto();
        domainTemplateDto.setId(id);
        domainTemplateDto.setAbbreviation(abbreviation);
        domainTemplateDto.setName(name);
        domainTemplateDto.setDescription(description);
        domainTemplateDto.setAuthority(authority);
        domainTemplateDto.setRevision(revision);
        domainTemplateDto.setTemplateVersion(templateVersion);
        domainTemplateDto.setElementTypeDefinitions(elementTypeDefinitions);
        domainTemplateDto.setCatalogs(catalogs);
        return domainTemplateDto;
    }

    public void setElementTypeDefinitions(
            Map<String, ElementTypeDefinitionDto> elementTypeDefinitions) {
        this.elementTypeDefinitions = elementTypeDefinitions;
    }

    /**
     * Create a catalog from a set of element DTOs. The catalog is stored in the
     * assembler and will be added to the domain template when
     * {@link #createDomainTemplateDto()} is called.
     */
    public void addCatalog(String catalogName, String elementPrefix,
            Map<String, AbstractElementDto> catalogElementById) {
        String catalogId = Key.newUuid()
                              .uuidValue();
        Map<String, TransformCatalogItemDto> catalogItemsById = createCatalogItems(catalogElementById,
                                                                                   catalogId,
                                                                                   elementPrefix);

        for (Map.Entry<String, AbstractElementDto> e : catalogElementById.entrySet()) {
            createTailoringReferences(e.getValue(), catalogItemsById);
            createExternalTailoringReferences(e.getKey(), catalogItemsById);
        }
        catalogItemsById.values()
                        .stream()
                        .forEach(i -> i.getElement()
                                       .getLinks()
                                       .clear());
        TransformCatalogDto catalogDto = new TransformCatalogDto();
        catalogDto.setName(catalogName);
        catalogDto.setId(catalogId);
        catalogDto.getCatalogItems()
                  .addAll(catalogItemsById.values());
        catalogDto.setDomainTemplate(new SyntheticIdRef<>(id, DomainTemplate.class, assembler));

        catalogs.add(catalogDto);
    }

    private Map<String, TransformCatalogItemDto> createCatalogItems(
            Map<String, AbstractElementDto> readElements, String catalogId, String elementPrefix) {
        Map<String, TransformCatalogItemDto> cache = new HashMap<>();
        for (Map.Entry<String, AbstractElementDto> e : readElements.entrySet()) {
            TransformCatalogItemDto itemDto = new TransformCatalogItemDto();
            itemDto.setTailoringReferences(new HashSet<>());
            itemDto.setElement(e.getValue());
            itemDto.setCatalog(SyntheticIdRef.from(catalogId, Catalog.class));
            AbstractElementDto elementDto = e.getValue();
            itemDto.setNamespace(elementPrefix + elementDto.getAbbreviation());
            itemDto.setId(Key.newUuid()
                             .uuidValue());
            elementDto.setOwner(SyntheticIdRef.from(itemDto.getId(), ElementOwner.class,
                                                    CatalogItem.class));
            elementDto.setType(SyntheticIdRef.toSingularTerm(elementDto.getModelInterface()));
            elementDto.associateWithTargetDomain(id);
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
                                               .equals(targetId))// exclude
                                                                 // self
                    .map(Entry::getValue)
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
                                TransformLinkTailoringReference referenceDto = new TransformLinkTailoringReference();
                                referenceDto.setCatalogItem(new SyntheticIdRef<>(targetItem.getId(),
                                        CatalogItem.class, assembler));
                                referenceDto.setReferenceType(TailoringReferenceType.LINK_EXTERNAL);
                                referenceDto.setLinkType(typedLinks.getKey());
                                referenceDto.setAttributes(new HashMap<>(link.getAttributes()));
                                targetItem.getTailoringReferences()
                                          .add(referenceDto);
                            });
                        }
                    });
    }

    private void createTailoringReferences(AbstractElementDto value,
            Map<String, TransformCatalogItemDto> catalogItems) {
        TransformCatalogItemDto currentItem = catalogItems.get(((IdentifiableDto) value).getId());
        currentItem.setTailoringReferences(new HashSet<>());
        value.getLinks()
             .entrySet()
             .stream()
             .forEach(e -> e.getValue()
                            .forEach(l -> {
                                TransformCatalogItemDto itemDto = catalogItems.get(l.getTarget()
                                                                                    .getId());
                                TransformLinkTailoringReference referenceDto = new TransformLinkTailoringReference();
                                referenceDto.setCatalogItem(new SyntheticIdRef<>(itemDto.getId(),
                                        CatalogItem.class, assembler));
                                referenceDto.setReferenceType(TailoringReferenceType.LINK);
                                referenceDto.setLinkType(e.getKey());
                                referenceDto.setAttributes(new HashMap<>(l.getAttributes()));
                                currentItem.getTailoringReferences()
                                           .add(referenceDto);
                            }));
        CompositeEntityDto<?> e = (CompositeEntityDto<?>) value;
        e.getParts()
         .forEach(p -> {
             TransformCatalogItemDto itemDto = catalogItems.get(p.getId());
             CreateTailoringReferenceDto referenceDto = new CreateTailoringReferenceDto();
             referenceDto.setCatalogItem(new SyntheticIdRef<>(itemDto.getId(), CatalogItem.class,
                     assembler));
             currentItem.getTailoringReferences()
                        .add(referenceDto);
             referenceDto.setReferenceType(TailoringReferenceType.COPY);
         });
    }

    public Set<AbstractElementDto> processDemoUnit(Set<AbstractElementDto> readDemoUnitElements) {
        SyntheticIdRef<Domain> domainRef = new SyntheticIdRef<>(id, Domain.class, assembler);
        Set<IdRef<Domain>> domainsToApply = Collections.singleton(domainRef);
        readDemoUnitElements.forEach(e -> processDemoUnitElement(e, domainsToApply));
        return readDemoUnitElements;
    }

    private void processDemoUnitElement(AbstractElementDto element,
            Set<IdRef<Domain>> domainsToApply) {
        element.associateWithTargetDomain(id);
        element.setType(SyntheticIdRef.toSingularTerm(element.getModelInterface()));
        element.getCustomAspects()
               .values()
               .forEach(c -> c.setDomains(domainsToApply));
        element.getLinks()
               .values()
               .forEach(links -> links.forEach(link -> link.setDomains(domainsToApply)));
    }
}
