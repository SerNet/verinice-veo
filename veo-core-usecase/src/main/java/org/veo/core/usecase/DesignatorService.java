/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.core.usecase;

import org.veo.core.entity.Client;
import org.veo.core.entity.Designated;
import org.veo.core.repository.DesignatorSequenceRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DesignatorService {
  private final DesignatorSequenceRepository designatorSequenceRepository;

  /**
   * Assigns a designator to a new entity. Use before persisting the target.
   *
   * @throws IllegalStateException when the target already has a designator.
   */
  public void assignDesignator(Designated target, Client client) {
    if (target.getDesignator() != null) {
      throw new IllegalStateException(
          "Cannot reassign designator on target " + target.getDesignator());
    }
    String typeDesignator = target.getTypeDesignator();
    var number = designatorSequenceRepository.getNext(client.getId(), typeDesignator);
    target.setDesignator(typeDesignator + "-" + number);
  }
}
