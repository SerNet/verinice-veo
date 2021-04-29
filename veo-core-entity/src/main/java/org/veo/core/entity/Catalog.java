/*******************************************************************************
 * Copyright (c) 2021 Urs Zeidler.
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
package org.veo.core.entity;

import java.util.Set;

/**
 * A catalog is owned by a domain or domain Template. It contains a set of
 * predefined elements for this specific domain which can be applied to the
 * model. <br>
 * usecase: KEa.1 and KEa.2 These applied elements can be updated when a new
 * catalogitem in a new version of the domain says so.<br>
 * usecase: Update These applied elements can be compared with or transformed to
 * template elements from other domainTemplates. <br>
 * usecase: compliance mapping
 */
public interface Catalog extends ModelObject, Nameable {
    String SINGULAR_TERM = "catalog";
    String PLURAL_TERM = "catalogs";

    /**
     * All the template elements of this catalog.
     */
    Set<CatalogItem> getCatalogItems();

    default void setCatalogItems(Set<CatalogItem> catalogItems) {
        getCatalogItems().clear();
        catalogItems.forEach(catalogitem -> catalogitem.setCatalog(this));
        getCatalogItems().addAll(catalogItems);
    }

    /**
     * The owner of the catalog is always a domain template.
     */
    DomainTemplate getDomainTemplate();

    void setDomainTemplate(DomainTemplate aDomaintemplate);

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Catalog.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }
}
