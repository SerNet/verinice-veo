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
package org.veo.core.usecase.unit;

import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject.Lifecycle;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.impl.ClientImpl;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.common.CreationFailedException;
import org.veo.core.usecase.common.NameableInputData;
import org.veo.core.usecase.repository.ClientRepository;

/**
 * Create a new unit for a client. If a parentId is given, the unit will be
 * created as a subunit of an existing unit.
 *
 * If no client exists for the given client-ID it will be created. Users of this
 * class must ensure that a clientID belongs to a valid client - i.e. this class
 * is NOT the authoritative source to detemrine if a clientID is valid or not.
 *
 * Instead, this task should be carried out by an authentication service which
 * provides a valid clientID and passed on to this use case.
 *
 * @author akoderman
 */
// @Log
public class CreateUnitUseCase extends UseCase<CreateUnitUseCase.InputData, Unit> {

    private final ClientRepository clientRepository;

    public CreateUnitUseCase(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Unit execute(InputData input) {
        Client client = clientRepository.findById(input.getClientId())
                                        .orElse(new ClientImpl(input.getClientId(),
                                                input.getNameableInput()
                                                     .getName()));

        // Note: the new client will get the name of the new unit by default.
        // If we want to get a client name we would have to do a REST call to get it
        // from the auth server

        Unit newUnit;
        if (input.getParentUnitId()
                 .isEmpty()) {
            newUnit = client.createUnit(input.getNameableInput()
                                             .getName());
        } else {
            newUnit = client.getUnit(input.getParentUnitId()
                                          .get())
                            .orElseThrow(() -> new NotFoundException("Parent unit %s was not found",
                                    input.getParentUnitId()
                                         .get()))
                            .createSubUnit(input.getNameableInput()
                                                .getName());
        }
        newUnit.setAbbreviation(input.getNameableInput()
                                     .getAbbreviation());
        newUnit.setDescription(input.getNameableInput()
                                    .getDescription());
        newUnit.setState(Lifecycle.STORED_CURRENT);

        return clientRepository.save(client)
                               .getUnit(newUnit.getId())
                               .orElseThrow(() -> new CreationFailedException(
                                       "Could not save unit %s", input.nameableInput.getName()));
    }

    @Valid
    @Value
    public static class InputData {
        NameableInputData nameableInput;
        Key<UUID> clientId;
        Optional<Key<UUID>> parentUnitId;
    }
}
