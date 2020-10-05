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
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.repository.PersonRepository;

public class UpdatePersonUseCase<R> extends ModifyEntityUseCase<Person, R> {

    private final PersonRepository personRepository;

    public UpdatePersonUseCase(PersonRepository personRepository) {
        super();
        this.personRepository = personRepository;
    }

    @Override
    public OutputData<Person> performModification(InputData<Person> input) {
        Person storedPerson = personRepository.findById(input.getEntity()
                                                             .getId())
                                              .orElseThrow(() -> new NotFoundException(
                                                      "Person %s was not found.", input.getEntity()
                                                                                       .getId()
                                                                                       .uuidValue()));
        checkETag(storedPerson, input);
        Person person = input.getEntity();
        person.setVersion(storedPerson.getVersion());
        person.setValidFrom(Instant.now());
        checkClientBoundaries(input, storedPerson);
        return new OutputData<>(personRepository.save(person));

    }

}
