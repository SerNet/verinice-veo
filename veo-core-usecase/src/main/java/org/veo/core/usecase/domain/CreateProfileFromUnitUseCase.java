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
package org.veo.core.usecase.domain;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ProfileRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.extern.slf4j.Slf4j;

/** Create or update a profile in a domain. */
@Slf4j
public class CreateProfileFromUnitUseCase
    extends AbstractCreateItemsFromUnitUseCase<ProfileItem, Profile>
    implements TransactionalUseCase<
        CreateProfileFromUnitUseCase.InputData, CreateProfileFromUnitUseCase.OutputData> {

  private final ProfileRepository profileRepo;

  public CreateProfileFromUnitUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository,
      EntityFactory factory,
      ProfileRepository profileRepo) {
    super(factory, domainRepository, genericElementRepository, unitRepository);
    this.profileRepo = profileRepo;
  }

  public OutputData execute(InputData input) {
    Domain domain =
        domainRepository.getActiveById(input.domainId, input.authenticatedClient.getId());
    Client client = input.authenticatedClient;
    Profile profile =
        input.profileId == null
            ? factory.createProfile(domain)
            : domain.getProfiles().stream()
                .filter(p1 -> p1.getId().equals(input.profileId))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

    profile.setName(input.name);
    profile.setDescription(input.description);
    profile.setLanguage(input.language);

    if (input.unitId != null) {
      cleanProfile(profile);
      var unit = unitRepository.getById(input.unitId);
      unit.checkSameClient(client);

      Map<Element, ProfileItem> elementsToProfileItems =
          getElements(unit, domain).stream()
              .collect(Collectors.toMap(Function.identity(), e -> e.toProfileItem(profile)));
      createTailorreferences(elementsToProfileItems, domain);
      profile.getItems().addAll(elementsToProfileItems.values());
    }

    Profile p = profileRepo.save(profile);
    log.info(
        "new profile {} in domain {} with {} elements created",
        profile.getName(),
        domain.getName(),
        profile.getItems().size());
    return new OutputData(p);
  }

  private void cleanProfile(Profile profile) {
    profile.getItems().clear();
  }

  private Set<Element> getElements(Unit unit, Domain domain) {
    var query = genericElementRepository.query(unit.getClient());
    query.whereUnitIn(Set.of(unit));
    query.whereDomainsContain(domain);
    query.fetchAppliedCatalogItems();
    query.fetchRisks();
    query.fetchRiskValuesAspects();
    var elements = new HashSet<>(query.execute(PagingConfiguration.UNPAGED).getResultPage());
    return elements;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Valid
  public record InputData(
      Key<UUID> domainId,
      Client authenticatedClient,
      Key<UUID> unitId,
      Key<UUID> profileId,
      String name,
      String description,
      String language)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Profile profile) implements UseCase.OutputData {}
}
