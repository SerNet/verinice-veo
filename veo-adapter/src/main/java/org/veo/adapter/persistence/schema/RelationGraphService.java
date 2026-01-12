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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.GraphNodeDto;
import org.veo.adapter.presenter.api.dto.GraphResultDto;
import org.veo.adapter.presenter.api.dto.RelationDto;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.InOrOutboundLink;
import org.veo.core.entity.LinkDirection;
import org.veo.core.entity.Scope;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.specification.ResultSizeExceededException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
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

  @Value("${veo.graph.max-neighbors:30}")
  private int maxGraphNeighbors;

  @Transactional
  public GraphResultDto getGraph(UUID id, UUID domainId, ElementType elementType, Locale locale) {
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

    List<RelationDto> relationDtos = new ArrayList<>();
    Set<Element> allElements = new HashSet<>();
    allElements.add(centerEl);

    addCustomLinksRelations(centerEl, domain, locale, allElements, relationDtos);
    addScopeRelations(centerEl, domain, locale, allElements, relationDtos);
    addCompositeRelations(centerEl, domain, locale, allElements, relationDtos);

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
    return new GraphResultDto(allEntities, relationDtos);
  }

  private String urlBuild(Element el, UUID domainId) {
    return urlAssembler.elementInDomainRefOf(
        TypedId.from(el.getId(), el.getType().getTypeStrict()), domainId);
  }

  private String buildPartsMembersLabel(Locale locale) {
    return locale.getLanguage().startsWith("de") ? "enthält" : "contains";
  }

  private void checkLimit(Set<Element> allElements) {
    int neighborCount = allElements.size() - 1;

    if (neighborCount > maxGraphNeighbors) {
      throw new ResultSizeExceededException("Too many related elements");
    }
  }

  private void addCustomLinksRelations(
      Element centerEl,
      Domain domain,
      Locale locale,
      Set<Element> allElements,
      List<RelationDto> relationDtos) {
    LinkQuery linkQuery = genericRepository.queryLinks(centerEl, domain);

    PagingConfiguration<LinkQuery.SortCriterion> linkPaging =
        new PagingConfiguration<>(
            maxGraphNeighbors + 1,
            0,
            LinkQuery.SortCriterion.DIRECTION,
            PagingConfiguration.SortOrder.ASCENDING);
    var translations = entitySchemaService.findTranslations(Set.of(domain), Set.of(locale));
    List<InOrOutboundLink> customLinks = linkQuery.execute(linkPaging).resultPage();
    for (InOrOutboundLink customLink : customLinks) {

      Element linkedEl = customLink.linkedElement();
      String linkType = customLink.linkType();

      allElements.add(linkedEl);
      checkLimit(allElements);

      String sourceRef;
      String targetRef;

      if (customLink.direction() == LinkDirection.OUTBOUND) {
        sourceRef = urlBuild(centerEl, domain.getId());
        targetRef = urlBuild(linkedEl, domain.getId());
      } else {
        sourceRef = urlBuild(linkedEl, domain.getId());
        targetRef = urlBuild(centerEl, domain.getId());
      }

      relationDtos.add(
          new RelationDto(
              RelationDto.RelationType.CUSTOM_LINK,
              sourceRef,
              targetRef,
              translations.get(locale, linkType).orElse(linkType)));
    }
  }

  private void addScopeRelations(
      Element centerEl,
      Domain domain,
      Locale locale,
      Set<Element> allElements,
      List<RelationDto> relationDtos) {
    if (centerEl instanceof Scope centerScope) {
      for (Element member : centerScope.getMembers()) {
        if (!member.getDomains().contains(domain)) {
          continue;
        }

        allElements.add(member);
        checkLimit(allElements);

        relationDtos.add(
            new RelationDto(
                RelationDto.RelationType.PART_OR_MEMBER,
                urlBuild(centerEl, domain.getId()),
                urlBuild(member, domain.getId()),
                buildPartsMembersLabel(locale)));
      }
    }

    for (Scope scope : centerEl.getScopes()) {
      if (!scope.getDomains().contains(domain)) {
        continue;
      }

      allElements.add(scope);
      checkLimit(allElements);

      relationDtos.add(
          new RelationDto(
              RelationDto.RelationType.PART_OR_MEMBER,
              urlBuild(scope, domain.getId()),
              urlBuild(centerEl, domain.getId()),
              buildPartsMembersLabel(locale)));
    }
  }

  private void addCompositeRelations(
      Element centerEl,
      Domain domain,
      Locale locale,
      Set<Element> allElements,
      List<RelationDto> relationDtos) {
    if (centerEl instanceof CompositeElement<?> compositeEl) {
      for (Element part : compositeEl.getParts()) {
        if (!part.getDomains().contains(domain)) {
          continue;
        }

        allElements.add(part);
        checkLimit(allElements);

        relationDtos.add(
            new RelationDto(
                RelationDto.RelationType.PART_OR_MEMBER,
                urlBuild(centerEl, domain.getId()),
                urlBuild(part, domain.getId()),
                buildPartsMembersLabel(locale)));
      }

      for (Element composite : compositeEl.getComposites()) {
        if (!composite.getDomains().contains(domain)) {
          continue;
        }

        allElements.add(composite);
        checkLimit(allElements);

        relationDtos.add(
            new RelationDto(
                RelationDto.RelationType.PART_OR_MEMBER,
                urlBuild(composite, domain.getId()),
                urlBuild(centerEl, domain.getId()),
                buildPartsMembersLabel(locale)));
      }
    }
  }
}
