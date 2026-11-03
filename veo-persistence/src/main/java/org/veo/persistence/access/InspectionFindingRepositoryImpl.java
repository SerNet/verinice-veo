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
package org.veo.persistence.access;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.InspectionFindingRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.persistence.access.jpa.InspectionFindingDataRepository;
import org.veo.persistence.entity.jpa.InspectionFindingData;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InspectionFindingRepositoryImpl implements InspectionFindingRepository {

  private final InspectionFindingDataRepository inspectionFindingDataRepository;
  private final GenericElementRepository genericElementRepository;

  @Override
  public List<Finding> findInspections(Element element, Domain domain) {
    var findings = inspectionFindingDataRepository.findAllByElementIsAndDomainIs(element, domain);
    var inspections = domain.getInspections();
    return findings.stream()
        .map(it -> inspections.get(it.getInspectionId()))
        .map(inspection ->
            new Finding(
                inspection.getSeverity(), inspection.getDescription(), inspection.getSuggestions()))
        .toList();
  }

  @Override
  public void updateInspections(Element element, Domain domain, boolean deleteExistingInspections) {
    if (deleteExistingInspections) {
      inspectionFindingDataRepository.deleteAllByElementIsAndDomainIs(element, domain);
    }
    var inspections = domain.getInspections();
    inspectionFindingDataRepository.saveAll(getFindings(element, domain, inspections));
  }

  @Override
  public void updateInspections(Domain domain, boolean deleteExistingInspections) {
    if (deleteExistingInspections) {
      removeInspections(domain);
    }
    domain.getInspections().forEach((k, v) -> updateInspections(domain, k, v, false));
  }

  @Override
  public void updateInspections(
      Domain domain,
      String inspectionId,
      Inspection inspection,
      boolean deleteExistingInspections) {
    if (deleteExistingInspections) {
      removeInspections(domain, inspectionId);
    }

    var query = genericElementRepository.query(domain.getOwner());
    query.whereDomainsContain(domain);

    if (inspection.getElementType() != null) {
      query.whereElementTypeMatches(new QueryCondition<>(Set.of(inspection.getElementType())));
    }
    if (inspection.getElementSubType() != null) {
      query.whereSubTypeMatches(
          new QueryCondition<>(Set.of(inspection.getElementSubType())), domain);
    }

    var elements = query.execute(PagingConfiguration.UNPAGED).resultPage();

    inspectionFindingDataRepository.saveAll(
        getFindings(domain, inspectionId, inspection, elements));
  }

  private static @NonNull List<InspectionFindingData> getFindings(
      Domain domain, String inspectionId, Inspection inspection, List<Element> elements) {
    return elements.stream()
        .flatMap(e -> getFindings(e, domain, Map.of(inspectionId, inspection)).stream())
        .toList();
  }

  private static @NonNull List<InspectionFindingData> getFindings(
      Element element, Domain domain, Map<String, Inspection> inspections) {
    return inspections.entrySet().stream()
        .map(e ->
            e.getValue()
                .run(element, domain)
                .map(_ -> {
                  var d = new InspectionFindingData();
                  d.setInspectionId(e.getKey());
                  d.setDomain(domain);
                  d.setElement(element);
                  d.setUnit(element.getOwner());
                  return d;
                }))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  public void removeInspections(Domain domain) {
    inspectionFindingDataRepository.deleteAllByDomainIs(domain);
  }

  @Override
  public void removeInspections(Domain domain, String inspectionId) {
    inspectionFindingDataRepository.deleteAllByDomainIsAndInspectionIdIs(domain, inspectionId);
  }
}
