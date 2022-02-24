/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
import org.veo.core.repository.ScopeRepository;
import org.veo.core.usecase.base.ModifyElementUseCase;

public class UpdateScopeUseCase extends ModifyElementUseCase<Scope> {

    public UpdateScopeUseCase(ScopeRepository scopeRepository) {
        super(scopeRepository);
    }

    @Override
    protected void validate(Scope oldElement, Scope newElement) {
        oldElement.getDomains()
                  .forEach(domain -> {
                      oldElement.getRiskDefinition(domain)
                                .ifPresent(oldRiskDef -> {
                                    newElement.getRiskDefinition(domain)
                                              .ifPresentOrElse(newRiskDef -> {
                                                  if (!oldRiskDef.equals(newRiskDef)) {
                                                      throw new IllegalArgumentException(
                                                              String.format("Cannot update existing risk definition reference from scope %s",
                                                                            oldElement.getIdAsString()));
                                                  }
                                              }, () -> {
                                                  throw new IllegalArgumentException(
                                                          String.format("Cannot remove existing risk definition reference from scope %s",
                                                                        oldElement.getIdAsString()));
                                              });
                                });
                  });
    }
}
