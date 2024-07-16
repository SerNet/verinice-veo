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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.PersonRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.ControlImplementationDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.PersonData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class PersonRepositoryImpl extends AbstractCompositeEntityRepositoryImpl<Person, PersonData>
    implements PersonRepository {

  private final AssetDataRepository assetDataRepository;
  private final ProcessDataRepository processDataRepository;
  private final ScopeDataRepository scopeDataRepository;
  private final ControlImplementationDataRepository controlImplementationDataRepository;
  private final RequirementImplementationDataRepository requirementImplementationDataRepository;

  public PersonRepositoryImpl(
      PersonDataRepository dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      AssetDataRepository assetDataRepository,
      ProcessDataRepository processDataRepository,
      ElementQueryFactory elementQueryFactory,
      ControlImplementationDataRepository controlImplementationDataRepository,
      RequirementImplementationDataRepository requirementImplementationDataRepository) {
    super(
        dataRepository,
        validation,
        linkDataRepository,
        scopeDataRepository,
        elementQueryFactory,
        Person.class);
    this.assetDataRepository = assetDataRepository;
    this.processDataRepository = processDataRepository;
    this.scopeDataRepository = scopeDataRepository;
    this.controlImplementationDataRepository = controlImplementationDataRepository;
    this.requirementImplementationDataRepository = requirementImplementationDataRepository;
  }

  @Override
  public void deleteById(Key<UUID> id) {
    delete(dataRepository.findById(id.value()).orElseThrow());
  }

  @Override
  public void delete(Person person) {
    removeFromRisks(singleton((PersonData) person));
    removeFromCIRIs(singleton(person));
    super.deleteById(person.getId());
  }

  private void removeFromRisks(Set<PersonData> persons) {
    // remove association to person from risks:
    Stream.of(assetDataRepository, processDataRepository, scopeDataRepository)
        .flatMap(r -> r.findDistinctByRisks_RiskOwner_In(persons).stream())
        .flatMap(riskAffected -> riskAffected.getRisks().stream())
        .filter(risk -> persons.contains(risk.getRiskOwner()))
        .forEach(risk -> risk.appoint(null));
  }

  private void removeFromCIRIs(Set<Person> elements) {
    requirementImplementationDataRepository
        .findByPersons(elements)
        .forEach(
            ri -> {
              ri.setResponsible(null);
              ri.getOrigin().setUpdatedAt(Instant.now());
            });
    controlImplementationDataRepository
        .findByPersons(elements)
        .forEach(
            ci -> {
              ci.setResponsible(null);
              ci.getOwner().setUpdatedAt(Instant.now());
            });
  }

  @Override
  @Transactional
  public void deleteAll(Set<Person> elements) {
    removeFromRisks(elements.stream().map(PersonData.class::cast).collect(Collectors.toSet()));
    removeFromCIRIs(elements);

    // elements.stream().forEach(p -> removeFromCIsRIs(p));
    super.deleteAll(elements);
  }

  @Override
  public ElementQuery<Person> query(Client client) {
    return elementQueryFactory.queryPersons(client);
  }
}
