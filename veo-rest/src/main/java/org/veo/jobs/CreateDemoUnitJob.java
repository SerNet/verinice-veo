/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.jobs;

import org.veo.core.entity.Client;
import org.veo.core.usecase.unit.CreateDemoUnitUseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CreateDemoUnitJob {
  private final CreateDemoUnitUseCase createDemoUnitUseCase;

  public void createDemoUnitForClient(Client client) {
    AsSystemUser.runInClient(
        client,
        () -> createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(client.getId())));
  }
}
