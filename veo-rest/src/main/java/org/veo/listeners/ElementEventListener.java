/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.listeners;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import org.veo.core.entity.event.ElementEvent;
import org.veo.core.repository.GenericElementRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ElementEventListener {
  private final GenericElementRepository elementRepository;

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(ElementEvent event) {
    elementRepository
        .findById(event.getEntityId(), event.getEntityType(), event.getClientId())
        .ifPresent(element ->
            element.getDomains().stream()
                .filter(d -> event.getDomainId() == null || d.getId().equals(event.getDomainId()))
                .forEach(domain -> {
                  if (element.evaluateDecisions(domain, event)) {
                    element.setUpdatedAt(Instant.now());
                  }
                }));
  }
}
