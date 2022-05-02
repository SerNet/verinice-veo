/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo.core.usecase.base;

import org.veo.core.entity.Element;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.ElementRepository;

/**
 * Base class for get-elements use cases that use the default {@link GetElementsUseCase.InputData}
 */
public abstract class DefaultGetElementsUseCase<T extends Element>
    extends GetElementsUseCase<T, GetElementsUseCase.InputData> {

  protected DefaultGetElementsUseCase(
      ClientRepository clientRepository,
      ElementRepository<T> repository,
      UnitHierarchyProvider unitHierarchyProvider) {
    super(clientRepository, repository, unitHierarchyProvider);
  }
}
