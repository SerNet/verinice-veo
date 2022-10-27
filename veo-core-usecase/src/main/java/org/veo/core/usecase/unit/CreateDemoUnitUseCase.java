/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import static org.veo.core.entity.profile.ProfileRef.DEMO_UNIT_REF;
import static org.veo.core.service.DomainTemplateService.updateVersion;

import java.util.UUID;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.domain.ProfileApplier;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/** Create a new demo unit for a client. */
@RequiredArgsConstructor
@Slf4j
public class CreateDemoUnitUseCase
    implements TransactionalUseCase<
        CreateDemoUnitUseCase.InputData, CreateDemoUnitUseCase.OutputData> {
  private final ClientRepository clientRepository;
  private final EntityFactory entityFactory;
  private final ProfileApplier profileApplier;
  private final UnitRepository unitRepository;

  public static final String DEMO_UNIT_NAME = "Demo";

  @Override
  @Transactional
  public OutputData execute(InputData input) {
    var client = clientRepository.findById(input.getClientId()).orElseThrow();
    return new OutputData(createDemoUnitForClient(client));
  }

  private Unit createDemoUnitForClient(Client client) {
    var demoUnit = entityFactory.createUnit(DEMO_UNIT_NAME, null);
    updateVersion(demoUnit);
    demoUnit.setClient(client);
    client.incrementTotalUnits();
    unitRepository.save(demoUnit);
    client.getDomains().stream()
        .filter(d -> d.getDomainTemplate() != null)
        .forEach(d -> profileApplier.applyProfile(d, DEMO_UNIT_REF, demoUnit));
    return demoUnit;
  }

  @Override
  public Isolation getIsolation() {
    return Isolation.REPEATABLE_READ;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> clientId;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Unit unit;
  }
}
