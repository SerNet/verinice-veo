/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity.state;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.ControlImplementationConfiguration;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.NameAbbreviationAndDescription;
import org.veo.core.entity.ProfileState;
import org.veo.core.entity.TranslationProvider;
import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.domainmigration.DomainMigrationDefinition;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.riskdefinition.RiskDefinition;

public interface DomainBaseState
    extends EntityState, TranslationProvider<NameAbbreviationAndDescription> {

  UUID getId();

  @NotNull
  String getAuthority();

  Map<ElementType, ElementTypeDefinitionState> getElementTypeDefinitionStates();

  Map<String, Inspection> getInspections();

  Map<String, Decision> getDecisions();

  /** Returns a map of risk definitions grouped by their ID. */
  Map<String, RiskDefinition> getRiskDefinitions();

  /**
   * The default behavior when creating incarnation descriptions for catalog items from this domain
   */
  IncarnationConfiguration getIncarnationConfiguration();

  ControlImplementationConfiguration getControlImplementationConfiguration();

  DomainMigrationDefinition getDomainMigrationDefinition();

  String getTemplateVersion();

  Set<TemplateItemState<CatalogItem, DomainBase>> getCatalogItemStates();

  Set<ProfileState> getProfileStates();

  Class<? extends DomainBase> getModelInterface();
}
