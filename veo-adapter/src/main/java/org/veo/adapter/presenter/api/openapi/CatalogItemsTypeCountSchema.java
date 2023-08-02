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

import org.veo.core.entity.statistics.CatalogItemsTypeCount;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Amount of catalog items per sub type, grouped by element type",
    example = "{\"control\": {\"TOM\": 8}, \"scenario\": {\"Attack\": 13, \"NaturalDisaster\": 5}}",
    accessMode = Schema.AccessMode.READ_ONLY)
public final class CatalogItemsTypeCountSchema extends CatalogItemsTypeCount {
  private CatalogItemsTypeCountSchema() {}
}
