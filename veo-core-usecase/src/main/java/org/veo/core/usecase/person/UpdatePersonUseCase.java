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

import java.time.Instant;

import org.veo.core.entity.Person;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.repository.PersonRepository;

public class UpdatePersonUseCase extends ModifyEntityUseCase<Person> {

    private final PersonRepository personRepository;

    public UpdatePersonUseCase(PersonRepository personRepository,
            TransformContextProvider transformContextProvider) {
        super(transformContextProvider);
        this.personRepository = personRepository;
    }

    @Override
    public OutputData<Person> performModification(InputData<Person> input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        Person person = input.getEntity();
        person.setVersion(person.getVersion() + 1);
        person.setValidFrom(Instant.now());
        return new OutputData<>(personRepository.save(person, null, dataTargetToEntityContext));

    }

}
