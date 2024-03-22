/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.unit;

import org.veo.core.entity.Unit;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

public class UpdateUnitUseCase extends ChangeUnitUseCase {

  public UpdateUnitUseCase(
      UnitRepository repository,
      UnitValidator unitValidator,
      EntityStateMapper entityStateMapper,
      RefResolverFactory refResolverFactory) {
    super(repository, unitValidator);
    this.entityStateMapper = entityStateMapper;
    this.refResolverFactory = refResolverFactory;
  }

  private final RefResolverFactory refResolverFactory;
  private final EntityStateMapper entityStateMapper;

  @Override
  protected Unit update(Unit storedUnit, ChangeUnitUseCase.InputData input) {
    entityStateMapper.mapState(
        input.getChangedUnit(), storedUnit, refResolverFactory.db(input.getAuthenticatedClient()));
    return storedUnit;
  }
}
