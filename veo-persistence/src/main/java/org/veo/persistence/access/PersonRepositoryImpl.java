/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import static java.util.Collections.singleton;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Unit;
import org.veo.core.repository.PersonRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.PersonData;

@Repository
public class PersonRepositoryImpl extends AbstractCompositeEntityRepositoryImpl<Person, PersonData>
        implements PersonRepository {

    private final AssetDataRepository assetDataRepository;
    private final ProcessDataRepository processDataRepository;

    public PersonRepositoryImpl(PersonDataRepository dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository, AssetDataRepository assetDataRepository,
            ProcessDataRepository processDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
        this.assetDataRepository = assetDataRepository;
        this.processDataRepository = processDataRepository;
    }

    @Override
    public void deleteById(Key<UUID> id) {
        delete(dataRepository.findById(id.uuidValue())
                             .orElseThrow());
    }

    @Override
    public void delete(Person person) {
        removeFromRisks(singleton((PersonData) person));
        super.deleteById(person.getId());
    }

    private void removeFromRisks(Set<PersonData> persons) {
        // remove association to person from risks:
        assetDataRepository.findDistinctByRisks_RiskOwner_In(persons)
                           .stream()
                           .flatMap(assetData -> assetData.getRisks()
                                                          .stream())
                           .filter(risk -> persons.contains(risk.getRiskOwner()))
                           .forEach(risk -> risk.appoint(null));

        processDataRepository.findDistinctByRisks_RiskOwner_In(persons)
                             .stream()
                             .flatMap(processData -> processData.getRisks()
                                                                .stream())
                             .filter(risk -> persons.contains(risk.getRiskOwner()))
                             .forEach(risk -> risk.appoint(null));
    }

    @Override
    @Transactional
    public void deleteByUnit(Unit owner) {
        removeFromRisks(dataRepository.findByUnits(singleton(owner.getId()
                                                                  .uuidValue())));
        super.deleteByUnit(owner);
    }
}