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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Scenario;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.AssetData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class AssetRepositoryImpl
    extends AbstractCompositeRiskAffectedRepository<Asset, AssetRisk, AssetData>
    implements AssetRepository {

  private final AssetDataRepository assetDataRepository;

  public AssetRepositoryImpl(
      AssetDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Asset.class);
    this.assetDataRepository = dataRepository;
  }

  @Override
  public ElementQuery<Asset> query(Client client) {
    return elementQueryFactory.queryAssets(client);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Asset> findWithRisksAndScenarios(Set<UUID> ids) {
    var elements = assetDataRepository.findWithRisksAndScenariosByIdIn(ids);
    return Collections.unmodifiableSet(elements);
  }

  @Override
  public Set<Asset> findRisksWithValue(Scenario scenario) {
    return new HashSet<>(
        ((AssetDataRepository) dataRepository)
            .findRisksWithValue(singleton(((ScenarioData) scenario))));
  }

  @Override
  public Optional<Asset> findByIdWithRiskValues(UUID id, UserAccessRights rights) {
    return assetDataRepository
        .findByIdsWithRiskValues(
            Set.of(id),
            rights.clientId(),
            rights.isUnitAccessResticted(),
            rights.getReadableUnitIds())
        .stream()
        .findFirst()
        .map(Asset.class::cast);
  }
}
