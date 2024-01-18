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
package org.veo.core.usecase.catalogitem;

import static java.util.function.Function.identity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.ModelConsistencyException;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.TailoringReferenceParameterState;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.AbstractTemplateItemRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.domain.ElementBatchCreator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class IncarnationDescriptionApplier {
  private final EntityFactory factory;
  private final UnitRepository unitRepository;
  private final ElementBatchCreator elementBatchCreator;
  private final RepositoryProvider repositoryProvider;

  public <T extends TemplateItem<T>> List<Element> incarnate(
      Key<UUID> unitId,
      List<TemplateItemIncarnationDescriptionState> descriptions,
      AbstractTemplateItemRepository<T> repository,
      Client client) {
    log.info("Incarnating {} template items", descriptions.size());
    var unit = unitRepository.getByIdFetchClient(unitId);
    unit.checkSameClient(client);
    var referencedElementsById = loadReferencedElements(descriptions, client);
    var itemsById = loadItems(descriptions, repository, client);
    var tailoringReferencesById = loadTailoringReferences(descriptions, repository, client);
    var newElements =
        incarnate(descriptions, unit, itemsById, referencedElementsById, tailoringReferencesById);
    Stream.concat(newElements.stream(), referencedElementsById.values().stream())
        .forEach(DomainSensitiveElementValidator::validate);
    return newElements;
  }

  private static <T extends TemplateItem<T>> Map<String, T> loadItems(
      List<TemplateItemIncarnationDescriptionState> descriptions,
      AbstractTemplateItemRepository<T> repository,
      Client client) {
    return repository
        .findAllByIdsFetchDomain(
            descriptions.stream()
                .map(TemplateItemIncarnationDescriptionState::getItemRef)
                .map(ITypedId::getId)
                .map(Key::uuidFrom)
                .collect(Collectors.toSet()),
            client)
        .stream()
        .collect(Collectors.toMap(Identifiable::getIdAsString, identity()));
  }

  private <T extends TemplateItem<T>> Map<String, TailoringReference<T>> loadTailoringReferences(
      List<TemplateItemIncarnationDescriptionState> descriptions,
      AbstractTemplateItemRepository<T> repo,
      Client client) {
    return repo
        .findTailoringReferencesByIds(
            descriptions.stream()
                .flatMap(d -> d.getParameterStates().stream())
                .map(TailoringReferenceParameterState::getId)
                .map(Key::uuidFrom)
                .collect(Collectors.toSet()),
            client)
        .stream()
        .collect(Collectors.toMap(Identifiable::getIdAsString, identity()));
  }

  private Map<String, Element> loadReferencedElements(
      List<TemplateItemIncarnationDescriptionState> descriptions, Client client) {
    return descriptions.stream()
        .flatMap(r -> r.getParameterStates().stream())
        .map(TailoringReferenceParameterState::getReferencedElementRef)
        .map(o -> o.orElse(null))
        .filter(Objects::nonNull)
        .distinct()
        .map(
            ref ->
                repositoryProvider
                    .getElementRepositoryFor(ref.getType())
                    .getById(Key.uuidFrom(ref.getId()), client.getId()))
        .collect(Collectors.toMap(Element::getIdAsString, identity()));
  }

  private <T extends TemplateItem<T>> List<Element> incarnate(
      List<TemplateItemIncarnationDescriptionState> descriptions,
      Unit unit,
      Map<String, T> itemsById,
      Map<String, Element> referencedElementsById,
      Map<String, TailoringReference<T>> tailoringReferencesById) {
    var elementsByItem =
        itemsById.values().stream().collect(Collectors.toMap(identity(), i -> i.incarnate(unit)));
    var sortedElements =
        descriptions.stream()
            .map(
                description -> {
                  var item = itemsById.get(description.getItemRef().getId());
                  var element = elementsByItem.get(item);
                  applyTailoringReferences(
                      item,
                      element,
                      description.getParameterStates(),
                      referencedElementsById,
                      elementsByItem,
                      tailoringReferencesById);
                  return element;
                })
            .toList();
    elementBatchCreator.create(sortedElements, unit, false);
    return sortedElements;
  }

  private <T extends TemplateItem<T>> void applyTailoringReferences(
      TemplateItem<T> item,
      Element element,
      List<TailoringReferenceParameterState> parameters,
      Map<String, Element> referencedElementsById,
      Map<T, Element> elementsByItem,
      Map<String, TailoringReference<T>> tailoringReferencesById) {
    parameters.forEach(
        parameter -> {
          var tailoringReference = tailoringReferencesById.get(parameter.getId());
          if (tailoringReference == null) {
            throw new NotFoundException(Key.uuidFrom(parameter.getId()), TailoringReference.class);
          }
          var target =
              parameter
                  .getReferencedElementRef()
                  .map(ITypedId::getId)
                  .map(referencedElementsById::get)
                  .orElseGet(() -> elementsByItem.get(tailoringReference.getTarget()));
          if (target == null) {
            throw new UnprocessableDataException(
                "%s %s not included in request but required by %s."
                    .formatted(
                        tailoringReference.getTarget().getModelInterface().getSimpleName(),
                        tailoringReference.getTarget().getName(),
                        item.getName()));
          }
          // TODO #898 adapt to non-redundant tailoring reference system
          applyTailoringReference(
              tailoringReference, element, target, item.requireDomainMembership(), elementsByItem);
        });
  }

  private <T extends TemplateItem<T>> void applyTailoringReference(
      TailoringReference<T> tailoringReference,
      Element origin,
      Element target,
      Domain domain,
      Map<T, Element> elementsByItem) {
    log.debug(
        "Applying {} tailoring reference {} with target {}",
        tailoringReference.getReferenceType(),
        tailoringReference.getIdAsString(),
        target.getDisplayName());
    switch (tailoringReference.getReferenceType()) {
      case LINK -> addLink(origin, target, domain, tailoringReference);
      case LINK_EXTERNAL -> addLink(target, origin, domain, tailoringReference);
      case PART -> addPart(origin, target);
      case COMPOSITE -> addPart(target, origin);
      case SCOPE -> addScope(origin, target);
      case MEMBER -> addScope(target, origin);
      case RISK -> addRisk(origin, target, domain, tailoringReference, elementsByItem);
      default ->
          throw new IllegalArgumentException(
              "Unexpected tailoring reference type %s"
                  .formatted(tailoringReference.getReferenceType()));
    }
  }

  private <T extends TemplateItem<T>> void addScope(Element origin, Element targetScope) {
    if (targetScope instanceof Scope scope) {
      scope.addMember(origin);
      handleModification(scope);
    }
  }

  private <T extends TemplateItem<T>> void addLink(
      Element source, Element target, Domain domain, TailoringReference<T> tailoringReference) {
    if (tailoringReference instanceof LinkTailoringReference<T> linkRef) {
      var link = factory.createCustomLink(target, source, linkRef.getLinkType(), domain);
      link.setAttributes(linkRef.getAttributes());
      if (source.applyLink(link)) {
        handleModification(source);
      }
    } else
      throw new ModelConsistencyException(
          "Cannot create link from %s"
              .formatted(tailoringReference.getModelInterface().getSimpleName()));
  }

  private static <T extends CompositeElement<T>> void addPart(Element composite, Element part) {
    if (!(composite instanceof CompositeElement<?>)) {
      throw new ModelConsistencyException(
          "Cannot add part to %s".formatted(composite.getModelType()));
    }
    if (!composite.getModelInterface().equals(part.getModelInterface())) {
      throw new ModelConsistencyException(
          "Cannot add %s as part of %s".formatted(part.getModelType(), composite.getModelType()));
    }
    if (((T) composite).addPart((T) part)) {
      handleModification(composite);
    }
  }

  private static <T extends TemplateItem<T>> void addRisk(
      Element source,
      Element target,
      Domain domain,
      TailoringReference<T> tailoringReference,
      Map<T, Element> elementsByItem) {
    if (source instanceof RiskAffected<?, ?> riskAffected && target instanceof Scenario scenario) {
      var risk = riskAffected.obtainRisk(scenario, domain);
      if (tailoringReference instanceof RiskTailoringReference<T> riskTailoringReference) {
        Optional.ofNullable(riskTailoringReference.getRiskOwner())
            .map(elementsByItem::get)
            .map(Person.class::cast)
            .ifPresent(risk::appoint);
        Optional.ofNullable(riskTailoringReference.getMitigation())
            .map(elementsByItem::get)
            .map(Control.class::cast)
            .ifPresent(risk::mitigate);
        risk.setValues(riskTailoringReference.getRiskDefinitions(), domain);
      }
    } else
      throw new ModelConsistencyException(
          "Cannot add risk for %s and %s".formatted(source.getModelType(), target.getModelType()));
  }

  private static void handleModification(Element element) {
    if (element.getId() != null) {
      element.setUpdatedAt(Instant.now());
    }
  }
}
