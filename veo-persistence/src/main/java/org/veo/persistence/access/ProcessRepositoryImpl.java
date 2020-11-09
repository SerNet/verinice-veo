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

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Process;
import org.veo.core.usecase.repository.ProcessRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.ProcessData;

@Repository
public class ProcessRepositoryImpl extends
        AbstractEntityLayerSupertypeRepository<Process, ProcessData> implements ProcessRepository {

    public ProcessRepositoryImpl(ProcessDataRepository dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository) {
        super(dataRepository, validation, linkDataRepository);
    }

}