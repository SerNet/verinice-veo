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

import org.veo.adapter.presenter.api.dto.CatalogableDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.ElementDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.presenter.api.response.transformer.SubTypeTransformer;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.util.CustomLinkComparators;
import org.veo.core.service.CatalogItemService;

public class CatalogItemServiceImpl implements CatalogItemService {
    private final DtoToEntityTransformer entityTransformer;
    private final EntityToDtoTransformer dtoTransformer;
    private final CatalogItemPrepareStrategy preparations;

    public CatalogItemServiceImpl(EntityToDtoTransformer dtoTransformer, EntityFactory factory,
            SubTypeTransformer subTypeTransformer, CatalogItemPrepareStrategy preparations) {
        this.dtoTransformer = dtoTransformer;
        this.preparations = preparations;

        this.entityTransformer = new DtoToEntityTransformer(factory,
                NoValidationSchemaLoader.NO_VALIDATION_LOADER, subTypeTransformer);
    }

    @Override
    public Catalogable createInstance(CatalogItem item, Domain domain) {
        Catalogable catalogElement = item.getElement();
        CatalogableDto dto = (CatalogableDto) dtoTransformer.transform2Dto(catalogElement);
        prepareDto(dto);
        PlaceholderResolver placeholderResolver = new PlaceholderResolver(entityTransformer);
        Catalogable newCatalogable = entityTransformer.transformDto2Catalogable(dto,
                                                                                placeholderResolver);
        preparations.prepareElement(domain, newCatalogable, false);
        if (catalogElement instanceof Element) {
            Element newELST = (Element) newCatalogable;
            Element catalogELST = (Element) catalogElement;
            catalogELST.getLinks()
                       .stream()
                       .sorted(CustomLinkComparators.BY_LINK_EXECUTION)
                       .forEach(orgLink -> newELST.addToLinks(copyLink(domain, placeholderResolver,
                                                                       orgLink)));
            catalogELST.getSubTypeAspects()
                       .forEach(st -> newELST.setSubType(domain, st.getSubType()));
            // TODO: VEO-612 handle parts
        }
        newCatalogable.setAppliedCatalogItems(Collections.singleton(item));
        return newCatalogable;
    }

    /**
     * Creates a copy of the link, with the domain set.
     */
    private CustomLink copyLink(Domain domain, PlaceholderResolver placeholderResolver,
            CustomLink catalogLink) {
        CustomLinkDto linkDto = dtoTransformer.transformCustomLink2Dto(catalogLink);
        linkDto.setTarget(null);
        CustomLink link = entityTransformer.transformDto2CustomLink(linkDto, catalogLink.getType(),
                                                                    NoValidationSchemaLoader.NO_VALIDATION,
                                                                    placeholderResolver);
        link.setTarget(catalogLink.getTarget());
        link.addToDomains(domain);
        return link;
    }

    /**
     * Prepare the dto for transformation by removing references, like links and
     * clears the domain.
     */
    private void prepareDto(CatalogableDto dto) {
        dto.setOwner(null);
        dto.getDomains()
           .clear();
        if (dto instanceof ElementDto) {
            ElementDto edto = (ElementDto) dto;
            edto.getLinks()
                .clear();
            edto.getSubType()
                .clear();
        }
    }

}
