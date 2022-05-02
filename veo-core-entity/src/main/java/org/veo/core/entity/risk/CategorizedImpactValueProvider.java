/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.core.entity.risk;

import java.util.List;

public interface CategorizedImpactValueProvider {

  List<Impact> getCategorizedImpacts();

  List<CategoryRef> getAvailableCategories();

  void setSpecificImpact(CategoryRef impactCategory, ImpactRef specific);

  ImpactRef getPotentialImpact(CategoryRef impactCategory);

  ImpactRef getSpecificImpact(CategoryRef impactCategory);

  ImpactRef getEffectiveImpact(CategoryRef impactCategory);

  String getSpecificImpactExplanation(CategoryRef impactCategory);

  void setPotentialImpact(CategoryRef impactCategory, ImpactRef potential);

  void setSpecificImpactExplanation(CategoryRef impactCategory, String explanation);
}
