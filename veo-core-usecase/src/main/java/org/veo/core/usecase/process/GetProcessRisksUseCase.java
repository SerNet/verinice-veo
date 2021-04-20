/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.process;

import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.risk.GetRisksUseCase;

public class GetProcessRisksUseCase extends GetRisksUseCase<Process, ProcessRisk> {
    public GetProcessRisksUseCase(RepositoryProvider repositoryProvider) {
        super(repositoryProvider, Process.class);
    }
}
