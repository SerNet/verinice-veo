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
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.repository.ControlRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.RiskAffectedDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.RiskAffectedData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ControlRepositoryImpl
    extends AbstractCompositeEntityRepositoryImpl<Control, ControlData>
    implements ControlRepository {

  private final RiskAffectedDataRepository<RiskAffectedData<?, ?>> riskAffectedDataRepository;

  public ControlRepositoryImpl(
      ControlDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory,
      RiskAffectedDataRepository<RiskAffectedData<?, ?>> riskAffectedDataRepository) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Control.class);
    this.riskAffectedDataRepository = riskAffectedDataRepository;
  }

  @Override
  public ElementQuery<Control> query(Client client) {
    return elementQueryFactory.queryControls(client);
  }

  @Override
  public void deleteById(UUID id) {
    delete(dataRepository.findById(id).orElseThrow());
  }

  @Override
  public void delete(Control control) {
    removeFromRisks(singleton((ControlData) control));
    removeImplementations(singleton((ControlData) control));
    super.deleteById(control.getId());
  }

  private void removeFromRisks(Set<ControlData> controls) {
    riskAffectedDataRepository.findDistinctByRisks_Mitigation_In(controls).stream()
        .flatMap(riskAffected -> riskAffected.getRisks().stream())
        .filter(risk -> controls.contains(risk.getMitigation()))
        .forEach(risk -> risk.mitigate(null));
  }

  private void removeImplementations(Iterable<ControlData> controls) {
    riskAffectedDataRepository
        .findAllByRequirementImplementationControls(controls)
        .forEach(
            ra ->
                controls.forEach(
                    control -> {
                      ra.disassociateControl(control);
                      ra.removeRequirementImplementation(control);
                    }));
  }

  @Override
  @Transactional
  public void deleteAll(Set<Control> elements) {
    var controls = elements.stream().map(ControlData.class::cast).collect(Collectors.toSet());
    removeFromRisks(controls);
    removeImplementations(controls);
    super.deleteAll(elements);
  }
}
