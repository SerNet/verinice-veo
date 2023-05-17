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
import org.veo.core.entity.Key;
import org.veo.core.repository.ControlRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ControlRepositoryImpl
    extends AbstractCompositeEntityRepositoryImpl<Control, ControlData>
    implements ControlRepository {

  private final AssetDataRepository assetDataRepository;
  private final ProcessDataRepository processDataRepository;

  public ControlRepositoryImpl(
      ControlDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      AssetDataRepository assetDataRepository,
      ProcessDataRepository processDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Control.class);
    this.assetDataRepository = assetDataRepository;
    this.processDataRepository = processDataRepository;
  }

  @Override
  public ElementQuery<Control> query(Client client) {
    return elementQueryFactory.queryControls(client);
  }

  @Override
  public void deleteById(Key<UUID> id) {
    delete(dataRepository.findById(id.uuidValue()).orElseThrow());
  }

  @Override
  public void delete(Control control) {
    removeFromRisks(singleton((ControlData) control));
    super.deleteById(control.getId());
  }

  private void removeFromRisks(Set<ControlData> controls) {
    // remove association to control from risks:
    assetDataRepository.findDistinctByRisks_Mitigation_In(controls).stream()
        .flatMap(assetData -> assetData.getRisks().stream())
        .filter(risk -> controls.contains(risk.getMitigation()))
        .forEach(risk -> risk.mitigate(null));

    processDataRepository.findDistinctByRisks_Mitigation_In(controls).stream()
        .flatMap(processData -> processData.getRisks().stream())
        .filter(risk -> controls.contains(risk.getMitigation()))
        .forEach(risk -> risk.mitigate(null));
  }

  @Override
  @Transactional
  public void deleteAll(Set<Control> elements) {
    removeFromRisks(elements.stream().map(ControlData.class::cast).collect(Collectors.toSet()));
    super.deleteAll(elements);
  }
}
