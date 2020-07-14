/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.core.usecase.person;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.CreateEntityInputData;
import org.veo.core.usecase.repository.PersonRepository;
import org.veo.core.usecase.repository.UnitRepository;

public class CreatePersonUseCase extends UseCase<CreateEntityInputData<Person>, Person> {

    private final UnitRepository unitRepository;
    private final TransformContextProvider transformContextProvider;
    private final PersonRepository personRepository;

    public CreatePersonUseCase(UnitRepository unitRepository, PersonRepository personRepository,
            TransformContextProvider transformContextProvider) {
        this.unitRepository = unitRepository;
        this.personRepository = personRepository;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public Person execute(CreateEntityInputData<Person> input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialClient()
                                                                                           .partialDomain();

        Unit unit = unitRepository.findById(input.getUnitId(), dataTargetToEntityContext)
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getUnitId()
                                               .uuidValue()));
        checkSameClient(input.getAuthenticatedClient(), unit, unit);

        Person person = input.getEntity();
        person.setId(Key.newUuid());
        return personRepository.save(person);
    }

}
