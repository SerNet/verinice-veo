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
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.TransformEntityToTargetContext;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.persistence.access.jpa.ClientDataRepository;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.UnitData;
import org.veo.persistence.entity.jpa.transformer.DataEntityToTargetContext;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;

@Repository
@AllArgsConstructor
public class ClientRepositoryImpl implements ClientRepository {

    // public Collection<ClientData> findByNameContainingIgnoreCase(String search);

    private ClientDataRepository dataRepository;

    private ModelObjectValidation validation;

    @Override
    public Client save(Client client, TransformEntityToTargetContext entityToDataContext,
            TransformTargetToEntityContext dataToEntityContext) {
        validation.validateModelObject(client);
        return dataRepository.save(ClientData.from(client, Optional.ofNullable(entityToDataContext)
                                                                   .orElseGet(DataEntityToTargetContext::getCompleteTransformationContext)))
                             .toClient(Optional.ofNullable(dataToEntityContext)
                                               .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext));
    }

    @Override
    public Optional<Client> findById(Key<UUID> id) {
        return findById(id, null);
    }

    @Override
    public Optional<Client> findById(Key<UUID> id,
            TransformTargetToEntityContext dataToEntityContext) {
        TransformTargetToEntityContext context = Optional.ofNullable(dataToEntityContext)
                                                         .orElseGet(DataTargetToEntityContext::getCompleteTransformationContext);

        boolean fetchUnits = ((DataTargetToEntityContext) context).getClientUnitsFunction() != null;
        Optional<ClientData> dataObject;
        if (fetchUnits) {
            dataObject = dataRepository.findByIdFetchUnits(id.uuidValue());
        } else {
            dataObject = dataRepository.findById(id.uuidValue());
        }
        return dataObject.map(data -> data.toClient(context));

    }

    @Override
    public List<Client> findByName(String search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Client entity) {
        dataRepository.delete(ClientData.from(entity));
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

    @Override
    public Set<Client> findClientsContainingUnit(Unit unit) {
        DataTargetToEntityContext context = DataTargetToEntityContext.getCompleteTransformationContext();
        return dataRepository.findDistinctByUnitsIn(Set.of(UnitData.from(unit)))
                             .stream()
                             .map(clientData -> clientData.toClient(context))
                             .collect(Collectors.toSet());
    }

}
