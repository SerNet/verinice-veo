/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.rest;

import static org.veo.rest.VeoRestConfiguration.PROFILE_BACKGROUND_TASKS;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.InspectionFindingRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile(PROFILE_BACKGROUND_TASKS)
@AllArgsConstructor
@Slf4j
public class DomainInspectionBackfill {

  private final DomainRepository domainRepository;
  private final InspectionFindingRepository inspectionFindingRepository;
  private final TransactionTemplate transactionTemplate;

  @EventListener(ApplicationReadyEvent.class)
  public void backfillInspectionFindings() {
    var domainIds =
        transactionTemplate.execute(
            status -> domainRepository.findIdsOfDomainsThatNeedInspectionBackfilling());
    log.info("Inspection finding backfill: {} domain(s) to process", domainIds.size());
    for (var domainId : domainIds) {
      try {
        transactionTemplate.executeWithoutResult(_ -> {
          var domain = domainRepository.getById(domainId);
          inspectionFindingRepository.updateInspections(domain, true);
          domain.setInspectionBackfillComplete(true);
        });
      } catch (RuntimeException e) {
        log.error("Inspection finding backfill failed for domain {}", domainId, e);
      }
    }
    log.info("Inspection finding backfill complete");
  }
}
