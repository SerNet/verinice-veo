/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.adapter;

import static org.springdoc.core.converters.AdditionalModelsConverter.replaceWithClass;

import org.veo.adapter.presenter.api.openapi.AddPartSuggestionSchema;
import org.veo.adapter.presenter.api.openapi.DecisionResultsSchema;
import org.veo.adapter.presenter.api.openapi.DecisionRuleRefSchema;
import org.veo.adapter.presenter.api.openapi.FindingSchema;
import org.veo.adapter.presenter.api.openapi.SuggestionSchema;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.decision.DecisionRuleRef;
import org.veo.core.entity.inspection.AddPartSuggestion;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.inspection.Suggestion;

/** Configures OpenAPI schema types globally. */
public class SchemaReplacer {
  public SchemaReplacer() {
    replaceWithClass(DecisionResult.class, DecisionResultsSchema.class);
    replaceWithClass(DecisionRuleRef.class, DecisionRuleRefSchema.class);
    replaceWithClass(Finding.class, FindingSchema.class);
    replaceWithClass(Suggestion.class, SuggestionSchema.class);
    replaceWithClass(AddPartSuggestion.class, AddPartSuggestionSchema.class);
  }
}
