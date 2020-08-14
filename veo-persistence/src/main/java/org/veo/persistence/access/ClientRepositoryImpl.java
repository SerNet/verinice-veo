/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.persistence.access;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.persistence.access.jpa.ClientDataRepository;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class ClientRepositoryImpl implements ClientRepository {

    private ClientDataRepository dataRepository;

    private ModelObjectValidation validation;

    @Override
    public Client save(Client client) {
        validation.validateModelObject(client);
        return dataRepository.save((ClientData) client);
    }

    @Override
    public Optional<Client> findById(Key<UUID> id) {
        Optional<ClientData> optional = dataRepository.findById(id.uuidValue());
        return optional.isPresent() ? Optional.of(optional.get()) : Optional.empty();
    }

    @Override
    public List<Client> findByName(String search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Client entity) {
        dataRepository.delete((ClientData) entity);
    }

    @Override
    public void deleteById(Key<UUID> id) {
        dataRepository.deleteById(id.uuidValue());
    }

    @Override
    public boolean exists(Key<UUID> id) {
        return dataRepository.existsById(id.uuidValue());
    }

    @Override
    public Set<Client> getByIds(Set<Key<UUID>> ids) {
        // TODO Auto-generated method stub
        return null;
    }

}
