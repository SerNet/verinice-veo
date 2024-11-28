/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.repository.ProfileRepository;
import org.veo.persistence.access.jpa.ProfileDataRepository;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.ProfileData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ProfileRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<Profile, ProfileData>
    implements ProfileRepository {

  private final ProfileDataRepository profileDataRepository;

  public ProfileRepositoryImpl(ProfileDataRepository dataRepository, ValidationService validator) {
    super(dataRepository, validator);
    profileDataRepository = dataRepository;
  }

  @Override
  public Optional<Profile> findProfileByIdFetchTailoringReferences(UUID profileId, UUID clientId) {
    return profileDataRepository
        .findProfileByIdFetchTailoringReferences(profileId, clientId)
        .map(Profile.class::cast);
  }

  @Override
  public List<ProfileItem> findItemsByIdsFetchDomainAndTailoringReferences(
      Set<UUID> profileItemIds, Client client) {
    return profileDataRepository.findItemsByIdsFetchDomainAndTailoringReferences(
        profileItemIds, client);
  }

  @Override
  public Optional<ProfileItem> findProfileItemByIdFetchTailoringReferences(
      UUID profileId, UUID itemId, UUID clientId) {
    return profileDataRepository.findProfileItemByIdFetchTailoringReferences(
        profileId, itemId, clientId);
  }

  @Override
  public Set<Profile> findAllByDomain(Domain domain) {
    return profileDataRepository.findAllByDomain((DomainData) domain);
  }

  @Override
  public List<ProfileItem> findItemsByProfileIdFetchDomainAndTailoringReferences(
      UUID profileId, Client client) {
    return profileDataRepository.findItemsByProfileIdFetchDomainAndTailoringReferences(
        profileId, client);
  }

  @Override
  public Set<Profile> findAllByDomainId(UUID clientId, UUID domainId) {
    return profileDataRepository.findAllByDomainId(clientId, domainId);
  }

  @Override
  public Optional<Profile> findById(UUID clientId, UUID profileId) {
    return profileDataRepository.findById(clientId, profileId);
  }
}
