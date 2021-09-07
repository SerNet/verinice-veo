/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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

import org.springframework.data.jpa.domain.Specification;

import org.veo.core.entity.Client;
import org.veo.core.entity.Process;
import org.veo.core.entity.Process.Status;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ProcessQuery;
import org.veo.core.repository.QueryCondition;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.entity.jpa.ProcessData;

/**
 * Implements {@link ElementQuery} using {@link Specification} API.
 */
public class ProcessQueryImpl extends ElementQueryImpl<Process, ProcessData>
        implements ProcessQuery {

    public ProcessQueryImpl(ProcessDataRepository repo, Client client) {
        super(repo, client);
    }

    @Override
    public ProcessQueryImpl whereStatusMatches(QueryCondition<Status> condition) {
        mySpec = mySpec.and((root, query,
                criteriaBuilder) -> in(root.get("status"), condition.getValues(), criteriaBuilder));
        return this;
    }

}
