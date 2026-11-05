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
package org.veo.core.repository;

import java.util.List;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.inspection.Inspection;

public interface InspectionFindingRepository {

  List<Finding> findInspections(Element element, Domain domain);

  void updateInspections(Element element, Domain domain, boolean deleteExistingInspections);

  default void updateInspections(Element element, boolean deleteExistingInspections) {
    element.getDomains().forEach(d -> updateInspections(element, d, deleteExistingInspections));
  }

  void updateInspections(Domain domain, boolean deleteExistingInspections);

  void updateInspections(
      Domain domain, String inspectionId, Inspection inspection, boolean deleteExistingInspections);

  void removeInspections(Domain domain);

  void removeInspections(Domain domain, String inspectionId);
}
