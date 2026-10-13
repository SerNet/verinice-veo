/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.persistence.access.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.persistence.entity.jpa.InspectionFindingData;

public interface InspectionFindingDataRepository
    extends CrudRepository<InspectionFindingData, UUID> {

  List<InspectionFindingData> findAllByElementIsAndDomainIs(Element element, Domain domain);

  void deleteAllByElementIsAndDomainIs(Element element, Domain domain);

  void deleteAllByDomainIs(Domain domain);

  void deleteAllByDomainIsAndInspectionIdIs(Domain domain, String inspectionId);
}
