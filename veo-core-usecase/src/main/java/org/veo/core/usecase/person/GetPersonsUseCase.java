/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.core.usecase.person;

import org.veo.core.entity.Person;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.PersonRepository;
import org.veo.core.usecase.base.DefaultGetEntitiesUseCase;
import org.veo.core.usecase.base.UnitHierarchyProvider;

/**
 * Reinstantiate persisted person objects.
 */
public class GetPersonsUseCase extends DefaultGetEntitiesUseCase<Person> {

    public GetPersonsUseCase(ClientRepository clientRepository, PersonRepository personRepository,
            UnitHierarchyProvider unitHierarchyProvider) {
        super(clientRepository, personRepository, unitHierarchyProvider);
    }
}