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
package org.veo.core.usecase.domain;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.profile.ProfileRef;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.DomainTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Applies a profile to a unit by instantiating all profile elements and risks inside the unit. */
@RequiredArgsConstructor
@Slf4j
public class ProfileApplier {

  private final DomainTemplateService domainTemplateService;
  private final UnitRepository unitRepository;
  private final ElementBatchCreator elementBatchCreator;

  public void applyProfile(Domain domain, ProfileRef profile, Unit unit) {
    var profileElements = domainTemplateService.getProfileElements(domain, profile);
    unit.addToDomains(domain);
    unitRepository.save(unit);
    elementBatchCreator.create(profileElements, unit);
    log.info("{} profile elements added to unit {}", profileElements.size(), unit.getIdAsString());
  }
}
