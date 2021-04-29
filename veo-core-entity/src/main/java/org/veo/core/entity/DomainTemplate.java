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
 * DomainTemplate The domaintemplare are managed by the system itself. The uuid
 * is a named uui generated as following:
 * https://v.de/veo/domain-templates/DOMAIN-NAME/VERSION DOMAIN-NAME:
 * authority-name VERSION: version.revision
 */
public interface DomainTemplate extends Nameable, ModelObject {
    String SINGULAR_TERM = "domaintemplate";
    String PLURAL_TERM = "domaintemplates";

    /**
     * The authority of this domaintemplate.
     */
    String getAuthority();

    void setAuthority(String aAuthority);

    /**
     * The version
     */
    String getTemplateVersion();

    void setTemplateVersion(String aTemplateVersion);

    /**
     * The revision of the version.
     */
    String getRevision();

    void setRevision(String aRevision);

    /**
     * The catalog describing the template element of this domaintemplate.
     */
    Set<Catalog> getCatalogs();

    default void setCatalogs(Set<Catalog> catalogs) {
        getCatalogs().clear();
        catalogs.forEach(catalog -> catalog.setDomainTemplate(this));
        getCatalogs().addAll(catalogs);
    }

    boolean addToCatalogs(Catalog aCatalog);

    void removeFromCatalog(Catalog aCatalog);

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return DomainTemplate.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }
}
