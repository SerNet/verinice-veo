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
package org.veo.adapter.presenter.api.openapi;

import java.util.List;
import java.util.Map;

import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.inspection.Severity;
import org.veo.core.entity.inspection.Suggestion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A problem or observation yielded by an inspection")
public abstract class FindingSchema extends Finding {
  public FindingSchema(
      Severity severity, Map<String, String> description, List<Suggestion> suggestions) {
    super(severity, description, suggestions);
  }

  @Override
  @Schema(description = "Key is ISO language key, value is human-readable description")
  public abstract Map<String, String> getDescription();

  @Override
  @Schema(description = "Rating of how problematic this finding is")
  public abstract Severity getSeverity();

  @Override
  @Schema(description = "Suggested actions that would fix this finding")
  public abstract List<Suggestion> getSuggestions();
}
