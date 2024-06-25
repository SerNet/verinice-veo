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
import org.veo.core.entity.ControlImplementationTailoringReference;
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
import org.veo.core.entity.SymIdentifiable;
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
import org.veo.core.repository.GenericElementRepository;
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
  private final GenericElementRepository elementRepository;

  public <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      List<Element> incarnate(
          Key<UUID> unitId,
          List<TemplateItemIncarnationDescriptionState<T, TNamespace>> descriptions,
          AbstractTemplateItemRepository<T, TNamespace> repository,
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

  private static <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      Map<String, T> loadItems(
          List<TemplateItemIncarnationDescriptionState<T, TNamespace>> descriptions,
          AbstractTemplateItemRepository<T, TNamespace> repository,
          Client client) {
    return repository
        .findAllByRefs(
            descriptions.stream()
                .map(TemplateItemIncarnationDescriptionState::getItemRef)
                .collect(Collectors.toSet()),
            client)
        .stream()
        .collect(Collectors.toMap(SymIdentifiable::getSymbolicIdAsString, identity()));
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      Map<String, TailoringReference<T, TNamespace>> loadTailoringReferences(
          List<TemplateItemIncarnationDescriptionState<T, TNamespace>> descriptions,
          AbstractTemplateItemRepository<T, TNamespace> repo,
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

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      Map<String, Element> loadReferencedElements(
          List<TemplateItemIncarnationDescriptionState<T, TNamespace>> descriptions,
          Client client) {
    return descriptions.stream()
        .flatMap(r -> r.getParameterStates().stream())
        .map(TailoringReferenceParameterState::getReferencedElementRef)
        .map(o -> o.orElse(null))
        .filter(Objects::nonNull)
        .distinct()
        .map(ref -> elementRepository.getById(Key.uuidFrom(ref.getId()), ref.getType(), client))
        .collect(Collectors.toMap(Element::getIdAsString, identity()));
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      List<Element> incarnate(
          List<TemplateItemIncarnationDescriptionState<T, TNamespace>> descriptions,
          Unit unit,
          Map<String, T> itemsById,
          Map<String, Element> referencedElementsById,
          Map<String, TailoringReference<T, TNamespace>> tailoringReferencesById) {
    var elementsByItemId =
        itemsById.values().stream()
            .collect(Collectors.toMap(T::getSymbolicIdAsString, i -> i.incarnate(unit)));
    var sortedElements =
        descriptions.stream()
            .map(
                description -> {
                  var item = itemsById.get(description.getItemRef().getSymbolicId());
                  var element = elementsByItemId.get(item.getSymbolicIdAsString());
                  applyTailoringReferences(
                      item,
                      element,
                      description.getParameterStates(),
                      referencedElementsById,
                      elementsByItemId,
                      tailoringReferencesById);
                  return element;
                })
            .toList();
    elementBatchCreator.create(sortedElements, unit, false);
    return sortedElements;
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void applyTailoringReferences(
          TemplateItem<T, TNamespace> item,
          Element element,
          List<TailoringReferenceParameterState> parameters,
          Map<String, Element> referencedElementsById,
          Map<String, Element> elementsByItemId,
          Map<String, TailoringReference<T, TNamespace>> tailoringReferencesById) {
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
                  .orElseGet(
                      () ->
                          elementsByItemId.get(
                              tailoringReference.getTarget().getSymbolicIdAsString()));
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
              tailoringReference,
              element,
              target,
              item.requireDomainMembership(),
              elementsByItemId);
        });
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void applyTailoringReference(
          TailoringReference<T, TNamespace> tailoringReference,
          Element origin,
          Element target,
          Domain domain,
          Map<String, Element> elementsByItemId) {
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
      case RISK -> addRisk(origin, target, domain, tailoringReference, elementsByItemId);
      case CONTROL_IMPLEMENTATION ->
          addControlImplementation(origin, target, tailoringReference, elementsByItemId);
      default ->
          throw new IllegalArgumentException(
              "Unexpected tailoring reference type %s"
                  .formatted(tailoringReference.getReferenceType()));
    }
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void addControlImplementation(
          Element origin,
          Element target,
          TailoringReference<T, TNamespace> tailoringReference,
          Map<String, Element> elementsByItemId) {
    if (origin instanceof RiskAffected<?, ?> ra
        && target instanceof Control control
        && tailoringReference
            instanceof ControlImplementationTailoringReference<T, TNamespace> tr) {
      var ci = ra.implementControl(control);
      ci.setDescription(tr.getDescription());
      Optional.ofNullable(tr.getResponsible())
          .map(T::getSymbolicIdAsString)
          .map(elementsByItemId::get)
          .ifPresent(
              responsible -> {
                if (responsible instanceof Person person) {
                  ci.setResponsible(person);
                } else
                  throw new ModelConsistencyException(
                      "Cannot use %s as responsible for control implementation"
                          .formatted(responsible.getModelType()));
              });
    } else
      throw new ModelConsistencyException(
          "Cannot create control implementation from %s for %s targeting %s"
              .formatted(
                  tailoringReference.getClass().getSimpleName(),
                  origin.getModelType(),
                  target.getModelType()));
  }

  private void addScope(Element origin, Element targetScope) {
    if (targetScope instanceof Scope scope) {
      scope.addMember(origin);
      handleModification(scope);
    }
  }

  private <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable> void addLink(
      Element source,
      Element target,
      Domain domain,
      TailoringReference<T, TNamespace> tailoringReference) {
    if (tailoringReference instanceof LinkTailoringReference<T, TNamespace> linkRef) {
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

  private static <T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
      void addRisk(
          Element source,
          Element target,
          Domain domain,
          TailoringReference<T, TNamespace> tailoringReference,
          Map<String, Element> elementsByItemId) {
    if (source instanceof RiskAffected<?, ?> riskAffected && target instanceof Scenario scenario) {
      var risk = riskAffected.obtainRisk(scenario, domain);
      if (tailoringReference
          instanceof RiskTailoringReference<T, TNamespace> riskTailoringReference) {
        Optional.ofNullable(riskTailoringReference.getRiskOwner())
            .map(T::getSymbolicIdAsString)
            .map(elementsByItemId::get)
            .map(Person.class::cast)
            .ifPresent(risk::appoint);
        Optional.ofNullable(riskTailoringReference.getMitigation())
            .map(T::getSymbolicIdAsString)
            .map(elementsByItemId::get)
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
