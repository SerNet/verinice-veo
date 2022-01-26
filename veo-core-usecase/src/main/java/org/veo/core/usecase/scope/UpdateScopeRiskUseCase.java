/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.scope;

import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.risk.UpdateRiskUseCase;

public class UpdateScopeRiskUseCase extends UpdateRiskUseCase<Scope, ScopeRisk> {
    public UpdateScopeRiskUseCase(RepositoryProvider repositoryProvider,
            EventPublisher eventPublisher) {
        super(repositoryProvider, Scope.class, eventPublisher);
    }
}
