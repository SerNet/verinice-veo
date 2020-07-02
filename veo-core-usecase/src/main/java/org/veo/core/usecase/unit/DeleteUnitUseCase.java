/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.UnitRepository;

public class DeleteUnitUseCase extends ChangeUnitUseCase {

    private final ClientRepository clientRepository;
    private final RepositoryProvider repositoryProvider;

    public DeleteUnitUseCase(ClientRepository clientRepository, UnitRepository unitRepository,
            TransformContextProvider transformContextProvider,
            RepositoryProvider repositoryProvider) {
        super(unitRepository, transformContextProvider);
        this.clientRepository = clientRepository;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    protected Unit update(Unit unit, InputData input) {
        removeUnitFromClient(unit);
        return unit;
    }

    private void removeUnitFromClient(Unit unit) {
        Set<Client> clients = clientRepository.findClientsContainingUnit(unit);
        clients.stream()
               .forEach(c -> c.removeUnit(unit));

        List.of(Asset.class, Control.class, Document.class, Person.class, Process.class)
            .forEach(clazz -> {
                repositoryProvider.getEntityLayerSupertypeRepositoryFor(clazz)
                                  .deleteByUnit(unit);
            });

        // Note: calling save() is required to trigger transformation back to data
        // entities which
        // are at this point managed by JPA. The save would otherwise not be necessary.
        clients.stream()
               .forEach(clientRepository::save);
    }

}
