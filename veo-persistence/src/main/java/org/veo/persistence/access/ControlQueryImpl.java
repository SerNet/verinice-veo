/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.persistence.access;

import java.util.List;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.entity.jpa.ControlData;

public class ControlQueryImpl extends CompositeElementQueryImpl<Control, ControlData> {

  private final ControlDataRepository controlRepository;

  public ControlQueryImpl(ControlDataRepository repo, Client client) {
    super(repo, client);
    this.controlRepository = repo;
  }

  @Override
  protected List<ControlData> fullyLoadItems(List<String> ids) {
    List<ControlData> result = super.fullyLoadItems(ids);

    if (fetchRiskValuesAspects) {
      controlRepository.findAllWithRiskValuesAspectsByDbIdIn(ids);
    }
    return result;
  }
}
