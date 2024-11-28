/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.repository.ProfileItemRepository;
import org.veo.persistence.access.jpa.ProfileItemDataRepository;
import org.veo.persistence.entity.jpa.ProfileItemData;
import org.veo.persistence.entity.jpa.ValidationService;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProfileItemRepositoryImpl implements ProfileItemRepository {

  private final ProfileItemDataRepository profileItemDataRepository;
  private final ValidationService validator;

  @Override
  public Set<ProfileItem> findAllByRefs(
      Set<ITypedSymbolicId<ProfileItem, ? extends Profile>> refs, Client client) {
    return refs.stream()
        .collect(Collectors.groupingBy(ITypedSymbolicId::getNamespaceId))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              var namespaceId = entry.getKey();
              var symIds =
                  entry.getValue().stream()
                      .map(ITypedSymbolicId::getSymbolicId)
                      .collect(Collectors.toSet());
              return profileItemDataRepository.findAllByIds(symIds, namespaceId, client).stream();
            })
        .collect(Collectors.toSet());
  }

  @Override
  public Set<TailoringReference<ProfileItem, Profile>> findTailoringReferencesByIds(
      Set<UUID> ids, Client client) {
    return profileItemDataRepository
        .findTailoringReferencesByIds(
            ids.stream().map(UUID::toString).collect(Collectors.toSet()), client)
        .stream()
        .map(tr -> (TailoringReference<ProfileItem, Profile>) tr)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ProfileItem> findAllByProfile(Profile profile, EntityType type) {
    return profileItemDataRepository.findAllByProfile(profile, type.getSingularTerm()).stream()
        .map(ProfileItem.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public ProfileItem save(ProfileItem item) {
    validator.validate(item);
    return profileItemDataRepository.save((ProfileItemData) item);
  }

  @Override
  public void saveAll(Collection<ProfileItem> templateItems) {
    templateItems.forEach(validator::validate);
    profileItemDataRepository.saveAll(
        templateItems.stream().map(ti -> (ProfileItemData) ti).toList());
  }
}
