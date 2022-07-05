/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
import org.veo.core.entity.Process;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.entity.jpa.ProcessData;

public class ProcessQueryImpl extends CompositeElementQueryImpl<Process, ProcessData> {

  private final boolean withRisks;
  private final ProcessDataRepository processRepository;

  public ProcessQueryImpl(ProcessDataRepository repo, Client client, boolean withRisks) {
    super(repo, client);
    this.withRisks = withRisks;
    this.processRepository = repo;
  }

  @Override
  protected List<ProcessData> fullyLoadItems(List<String> ids) {
    var items = super.fullyLoadItems(ids);
    if (withRisks) {
      processRepository.findAllWithRisksByDbIdIn(ids);
    }
    return items;
  }
}
