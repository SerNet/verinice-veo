/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.process;

import org.veo.core.entity.Process;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.usecase.base.GetElementUseCase;

/**
 * Reinstantiate a persisted process object.
 */
public class GetProcessUseCase extends GetElementUseCase<Process> {

    public GetProcessUseCase(ProcessRepository repository) {
        super(repository);
    }

}