/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.condition.CurrentElementExpression;
import org.veo.core.entity.condition.ImplementedRequirementsExpression;
import org.veo.core.entity.condition.LinkTargetsExpression;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

/**
 * The domain should be referenced by the domain objects if applicable. It defines a standard, a
 * best practice or a company-specific context. It can be bound to a domain template, which
 * represent an 'official' standard. The domain references the available customAspects, the forms
 * and other stuff needed to define a specific view on the data. For example the risk definition,
 * which describes what risk values exist and how they relate to each other.
 */
public interface Domain extends DomainBase, ClientOwned {

  String SINGULAR_TERM = "domain";
  String PLURAL_TERM = "domains";

  boolean isActive();

  void setActive(boolean aActive);

  DomainTemplate getDomainTemplate();

  void setDomainTemplate(DomainTemplate aDomaintemplate);

  @Override
  default Class<Domain> getModelInterface() {
    return Domain.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  void setOwner(Client owner);

  @NotNull
  Client getOwner();

  default Optional<Client> getOwningClient() {
    return Optional.ofNullable(getOwner());
  }

  /**
   * @return {@code true} if a new risk definition has been created or {@code false} if an existing
   *     risk definition has been updated.
   */
  boolean applyRiskDefinition(String riskDefinitionRef, RiskDefinition riskDefinition);

  void removeRiskDefinition(RiskDefinitionRef riskDefinition);

  default Map<String, Action> getActions() {
    // TODO #2844 use dynamic descriptive actions, remove hard-coded domain-specific biz
    if (this.getName().equals("IT-Grundschutz")) {
      return Map.of(
          "threatOverview",
          new Action(
              new TranslatedText(
                  Map.of(
                      Locale.GERMAN,
                      "Gefährdungsübersicht erstellen",
                      Locale.ENGLISH,
                      "Draw up threat overview")),
              Set.of(Asset.SINGULAR_TERM, Process.SINGULAR_TERM, Scope.SINGULAR_TERM),
              List.of(
                  new ApplyLinkTailoringReferences(
                      new ImplementedRequirementsExpression(new CurrentElementExpression()),
                      new IncarnationConfiguration(
                          IncarnationRequestModeType.MANUAL,
                          IncarnationLookup.NEVER,
                          // #852 only include LINKs.
                          // The LINK tailoring reference currently cannot be applied, because it
                          // originates on the control itself. The control is already incarnated,
                          // and
                          // we cannot apply tailoring references to existing incarnations yet. The
                          // workaround is to also include LINK_EXTERNAL, the reverse reference that
                          // can be applied successfully if the linked scenario does not exist yet.
                          // The current implementation cannot create the link if both the control
                          // and the scenario are already incarnated, that will only work with #852.
                          //                          Set.of(TailoringReferenceType.LINK,
                          // TailoringReferenceType.LINK_EXTERNAL),
                          Set.of(
                              TailoringReferenceType.COMPOSITE,
                              TailoringReferenceType.PART,
                              TailoringReferenceType.LINK_EXTERNAL),
                          null),
                      "control_relevantAppliedThreat"),
                  new AddRisksStep(
                      new LinkTargetsExpression(
                          new ImplementedRequirementsExpression(new CurrentElementExpression()),
                          "control_relevantAppliedThreat")))));
    }
    return Collections.emptyMap();
  }

  default Map<String, Action> getAvailableActions(Class<? extends Element> elementType) {
    return getActions().entrySet().stream()
        .filter(
            e ->
                e.getValue().elementTypes().contains(EntityType.getSingularTermByType(elementType)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  default Optional<Action> findAction(String actionId) {
    return Optional.ofNullable(getActions().get(actionId));
  }

  default String mapOldSubType(String subtype) {
    // TODO verinice-veo#3378: include mapping in migration definition
    return subtype;
  }

  default String mapOldStatus(String status) {
    // TODO verinice-veo#3378: include mapping in migration definition
    return status;
  }
}
