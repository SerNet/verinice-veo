/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core.entity.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ProfileState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientChangedEvent implements ClientEvent {

  @NotNull private final UUID clientId;
  @NotNull private final ClientChangeType type;
  private final Integer maxUnits;
  private final String name;

  /**
   * A list of domain names and profile product IDs that the client is licensed to use.
   *
   * <p>Keys are domain names ({@link Domain#getName()}), values are lists of profile product IDs
   * ({@link ProfileState#getProductId()}). When the client is created, the domains and profiles
   * specified here are added to the client.
   */
  private Map<String, List<String>> domainProducts;
}
