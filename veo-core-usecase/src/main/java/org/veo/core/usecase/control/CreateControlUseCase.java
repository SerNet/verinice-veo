/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core.usecase.control;

import org.veo.core.entity.Control;
import org.veo.core.repository.ControlRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.base.CreateElementUseCase;

public class CreateControlUseCase extends CreateElementUseCase<Control> {

    public CreateControlUseCase(UnitRepository unitRepository, ControlRepository entityRepo,
            DesignatorService designatorService) {
        super(unitRepository, entityRepo, designatorService);
    }

    @Override
    protected void validate(Control control) {
        // TODO VEO-1244 The same kind of validation as in UpdateControlUseCase should
        // be used here as soon as it is possible to create an element within a scope.
        control.getDomains()
               .forEach(domain -> {
                   if (control.getRiskValues(domain)
                              .map(rv -> !rv.isEmpty())
                              .orElse(false)) {
                       throw new IllegalArgumentException(
                               "Cannot create control with risk values, because it must a member of a scope with a risk definition first");
                   }
               });
    }
}
