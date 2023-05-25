/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.SubTypeStatusCount;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.ValidationService;

@Transactional(readOnly = true)
abstract class AbstractElementRepository<T extends Element, S extends ElementData>
    extends AbstractIdentifiableVersionedRepository<T, S> implements ElementRepository<T> {

  protected final ElementDataRepository<S> dataRepository;
  private final CustomLinkDataRepository linkDataRepository;

  final ScopeDataRepository scopeDataRepository;
  protected final ElementQueryFactory elementQueryFactory;
  final Class<T> elementType;

  AbstractElementRepository(
      ElementDataRepository<S> dataRepository,
      ValidationService validation,
      CustomLinkDataRepository linkDataRepository,
      ScopeDataRepository scopeDataRepository,
      ElementQueryFactory elementQueryFactory,
      Class<T> elementType) {
    super(dataRepository, validation);
    this.dataRepository = dataRepository;
    this.linkDataRepository = linkDataRepository;
    this.scopeDataRepository = scopeDataRepository;
    this.elementQueryFactory = elementQueryFactory;
    this.elementType = elementType;
  }

  @Override
  public Set<T> findByDomain(Domain domain) {
    var query = query(domain.getOwningClient().get());
    query.whereDomainsContain(domain);
    return new HashSet<>(query.execute(PagingConfiguration.UNPAGED).getResultPage());
  }

  @Override
  public Set<T> findByUnit(Unit owner) {
    var query = query(owner.getClient());
    query.whereOwnerIs(owner);
    query.fetchAppliedCatalogItems();
    query.fetchParentsAndChildrenAndSiblings();
    query.fetchRisks();
    query.fetchRiskValuesAspects();
    return new HashSet<>(query.execute(PagingConfiguration.UNPAGED).getResultPage());
  }

  @Transactional
  public void deleteAll(Set<T> elements) {
    Set<String> elementIds =
        elements.stream().map(Element::getIdAsString).collect(Collectors.toSet());
    deleteLinksByTargets(elementIds);

    elements.forEach(e -> e.getLinks().clear());
    elements.forEach(Element::remove);

    dataRepository.deleteAllById(elementIds);
  }

  @Override
  public Optional<T> findById(Key<UUID> id, Key<UUID> clientId) {
    return dataRepository.findById(id.uuidValue(), clientId.uuidValue()).map(e -> (T) e);
  }

  @Override
  public T getById(Key<UUID> id, Key<UUID> clientId) {
    return findById(id, clientId).orElseThrow(() -> new NotFoundException(id, elementType));
  }

  @Override
  public Set<SubTypeStatusCount> getCountsBySubType(Unit u, Domain d) {
    return dataRepository.getCountsBySubType(u.getIdAsString(), d.getIdAsString());
  }

  @Override
  @Transactional
  public void deleteById(Key<UUID> id) {
    deleteLinksByTargets(Set.of(id.uuidValue()));

    // remove element from scope members:
    Set<Scope> scopes =
        scopeDataRepository.findDistinctOthersByMemberIds(singleton(id.uuidValue()));
    scopes.stream().map(ScopeData.class::cast).forEach(scopeData -> scopeData.removeMemberById(id));

    dataRepository.deleteById(id.uuidValue());
  }

  private void deleteLinksByTargets(Set<String> targetElementIds) {
    // using deleteAll() to utilize batching and optimistic locking:
    var links = linkDataRepository.findLinksFromOtherElementsByTargetIds(targetElementIds);
    linkDataRepository.deleteAll(links);
    links.forEach(CustomLink::remove);
  }
}
