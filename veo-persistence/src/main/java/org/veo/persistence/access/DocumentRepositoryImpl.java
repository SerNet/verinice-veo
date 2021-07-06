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

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Document;
import org.veo.core.repository.DocumentRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.DocumentDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.DocumentData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

@Repository
public class DocumentRepositoryImpl
        extends AbstractCompositeEntityRepositoryImpl<Document, DocumentData>
        implements DocumentRepository {

    public DocumentRepositoryImpl(DocumentDataRepository dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
    }
}