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

import static java.util.Collections.singleton;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Control;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.ControlRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ControlData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;

@Repository
public class ControlRepositoryImpl extends
        AbstractCompositeEntityRepositoryImpl<Control, ControlData> implements ControlRepository {

    private final AssetDataRepository assetDataRepository;

    public ControlRepositoryImpl(ControlDataRepository dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository, AssetDataRepository assetDataRepository) {
        super(dataRepository, validation, linkDataRepository, scopeDataRepository);
        this.assetDataRepository = assetDataRepository;
    }

    @Override
    public void deleteById(Key<UUID> id) {
        delete(dataRepository.findById(id.uuidValue())
                             .orElseThrow());
    }

    @Override
    public void delete(Control control) {
        removeFromRisks(singleton((ControlData) control));
        super.deleteById(control.getId());
    }

    private void removeFromRisks(Set<ControlData> controls) {
        // remove association to control from risks:
        assetDataRepository.findDistinctByRisks_Mitigation_In(controls)
                           .stream()
                           .flatMap(assetData -> assetData.getRisks()
                                                          .stream())
                           .filter(risk -> controls.contains(risk.getMitigation()))
                           .forEach(risk -> risk.mitigate(null));
    }

    @Override
    @Transactional
    public void deleteByUnit(Unit owner) {
        removeFromRisks(dataRepository.findByUnits(singleton(owner.getDbId())));
        super.deleteByUnit(owner);
    }
}