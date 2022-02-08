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
package org.veo.core.entity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

/**
 * DomainTemplate The domaintemplare are managed by the system itself. The uuid
 * is a named uui generated as following:
 * https://v.de/veo/domain-templates/DOMAIN-NAME/VERSION DOMAIN-NAME:
 * authority-name VERSION: version.revision
 */
public interface DomainTemplate extends Nameable, Identifiable, Versioned {
    String SINGULAR_TERM = "domaintemplate";
    String PLURAL_TERM = "domaintemplates";

    int AUTHORITY_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
    int REVISION_MAX_LENGTH = Constraints.DEFAULT_STRING_MAX_LENGTH;
    int TEMPLATE_VERSION_MAX_LENGTH = 10;

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
    default Class<? extends Identifiable> getModelInterface() {
        return DomainTemplate.class;
    }

    @Override
    default String getModelType() {
        return SINGULAR_TERM;
    }

    Set<ElementTypeDefinition> getElementTypeDefinitions();

    void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions);

    default Optional<ElementTypeDefinition> getElementTypeDefinition(String type) {
        return getElementTypeDefinitions().stream()
                                          .filter(d -> d.getElementType()
                                                        .equals(type))
                                          .findFirst();
    }

    /**
     * Returns a map of risk definitions grouped by their ID.
     */
    Map<String, RiskDefinition> getRiskDefinitions();

    Optional<RiskDefinition> getRiskDefinition(String riskDefinitionId);

    void setRiskDefinitions(Map<String, RiskDefinition> definitions);
}
