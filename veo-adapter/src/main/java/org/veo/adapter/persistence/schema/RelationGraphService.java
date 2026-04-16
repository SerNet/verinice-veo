/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Alina Tsikunova
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
package org.veo.adapter.persistence.schema;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.GraphNodeDto;
import org.veo.adapter.presenter.api.dto.GraphResultDto;
import org.veo.adapter.presenter.api.dto.RelationDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.RelationRow;
import org.veo.core.service.EntitySchemaService;
import org.veo.core.service.UserAccessRightsProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RelationGraphService {

  private final DomainRepository domainRepository;
  private final EntitySchemaService entitySchemaService;
  private final ReferenceAssembler urlAssembler;
  private final UserAccessRightsProvider userAccessRightsProvider;
  private final GenericElementRepository genericRepository;

  @Transactional
  public GraphResultDto getGraph(
      UUID id, UUID domainId, ElementType elementType, Locale locale, Integer limit) {
    UUID clientId = userAccessRightsProvider.getAccessRights().getClientId();
    Domain domain = domainRepository.getById(domainId, clientId);

    ElementQuery<Element> query = genericRepository.query(domain.getOwner());

    query.whereIdIn(new QueryCondition<>(Set.of(id)));
    query.whereDomainsContain(domain);
    query.whereElementTypeMatches(new QueryCondition<>(Set.of(elementType)));

    List<Element> center = query.execute(PagingConfiguration.UNPAGED).resultPage();

    if (center.isEmpty()) {
      throw new NotFoundException("Element not found");
    }

    Element centerEl = center.get(0);

    List<RelationRow> rows =
        genericRepository.queryGraph(id, domainId, elementType, limit).execute();

    long totalCount = rows.isEmpty() ? 0 : rows.get(0).totalCount();

    List<UUID> neighborIds = rows.stream().map(RelationRow::neighborId).distinct().toList();

    List<Element> neighborElements = loadElementsByIds(domain, neighborIds);

    Set<Element> allElements = new HashSet<>();
    allElements.add(centerEl);
    allElements.addAll(neighborElements);

    Map<UUID, Element> elementsById =
        allElements.stream().collect(Collectors.toMap(Element::getId, Function.identity()));

    List<GraphNodeDto> allEntities =
        allElements.stream()
            .map(
                el ->
                    new GraphNodeDto(
                        urlBuild(el, domainId),
                        el.getDisplayName(),
                        el.getType(),
                        el.getSubType(domain),
                        el.getId()))
            .toList();

    var translations = entitySchemaService.findTranslations(Set.of(domain), Set.of(locale));

    List<RelationDto> relationDtos =
        rows.stream()
            .map(
                row -> {
                  String relationType = row.relationType();
                  UUID sourceId = row.sourceId();
                  UUID targetId = row.targetId();
                  String linkType = row.linkType();

                  Element source = elementsById.get(sourceId);
                  Element target = elementsById.get(targetId);

                  String label =
                      RelationDto.RelationType.CUSTOM_LINK.name().equals(relationType)
                          ? translations.get(locale, linkType).orElse(linkType)
                          : buildPartsMembersLabel(locale);

                  return new RelationDto(
                      RelationDto.RelationType.valueOf(relationType),
                      urlBuild(source, domainId),
                      urlBuild(target, domainId),
                      label);
                })
            .toList();

    return new GraphResultDto(allEntities, relationDtos, totalCount);
  }

  private String urlBuild(Element el, UUID domainId) {
    return urlAssembler.elementInDomainRefOf(
        TypedId.from(el.getId(), el.getType().getTypeStrict()), domainId);
  }

  private String buildPartsMembersLabel(Locale locale) {
    return locale.getLanguage().startsWith("de") ? "enthält" : "contains";
  }

  private List<Element> loadElementsByIds(Domain domain, List<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }

    ElementQuery<Element> query = genericRepository.query(domain.getOwner());
    query.whereIdIn(new QueryCondition<>(new HashSet<>(ids)));
    query.whereDomainsContain(domain);

    return query.execute(PagingConfiguration.UNPAGED).resultPage();
  }
}
