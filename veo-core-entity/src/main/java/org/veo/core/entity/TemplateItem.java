/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.entity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.TailoringReferenceState;
import org.veo.core.entity.state.TemplateItemState;

public interface TemplateItem<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TemplateItemState<T, TNamespace>, SymIdentifiable<T, TNamespace>, Versioned {
  void setElementType(ElementType aType);

  void setSubType(String subType);

  void setStatus(String status);

  Map<String, Map<String, Object>> getCustomAspects();

  @Override
  default Set<CustomAspectState> getCustomAspectStates() {
    return getCustomAspects().entrySet().stream()
        .map(kv -> new CustomAspectState.CustomAspectStateImpl(kv.getKey(), kv.getValue()))
        .collect(Collectors.toSet());
  }

  void setCustomAspects(Map<String, Map<String, Object>> container);

  Domain requireDomainMembership();

  Element incarnate(Unit owner);

  @Override
  default Set<TailoringReferenceState<T, TNamespace>> getTailoringReferenceStates() {
    return getTailoringReferences().stream()
        .map(tr -> (TailoringReferenceState<T, TNamespace>) tr)
        .collect(Collectors.toSet());
  }

  /** All the tailoring references for this template item. */
  Set<TailoringReference<T, TNamespace>> getTailoringReferences();

  void clearTailoringReferences();

  void setAspects(TemplateItemAspects aspects);

  default Class<? extends Element> getElementInterface() {
    return getElementType().getType();
  }

  DomainBase getDomainBase();

  /**
   * Includes itself together with {@link this.getElementsToCreate()}. This list is ordered. The
   * item itself is at the first position.
   */
  default List<T> getAllItemsToIncarnate() {
    return Stream.concat(
            Stream.of((T) this),
            getElementsToCreate().stream()
                .sorted(Comparator.comparing(SymIdentifiable::getSymbolicIdAsString))
                .distinct())
        .toList();
  }

  /**
   * Return the set additional elements to create. These elements are defined by {@link
   * TailoringReference} of type {@link TailoringReferenceType#COPY} or {@link
   * TailoringReferenceType#COPY_ALWAYS}.
   */
  default Set<T> getElementsToCreate() {
    Set<T> elementsToCreate = new HashSet<>();
    this.getTailoringReferences().stream()
        .filter(TailoringReference::isCopyRef)
        .forEach(r -> addElementsToCopy(r, elementsToCreate));
    return elementsToCreate;
  }

  default void addElementsToCopy(TailoringReference<T, TNamespace> reference, Set<T> itemList) {
    itemList.add(reference.getTarget());
    reference.getTarget().getTailoringReferences().stream()
        .filter(TailoringReference::isCopyRef)
        .forEach(rr -> addElementsToCopy(rr, itemList));
  }

  /**
   * Adds a new {@link TailoringReference} to this item. Use this method for reference types that
   * don't require additional data. For other types see {@link
   * TemplateItem#addLinkTailoringReference} & {@link TemplateItem#addRiskTailoringReference}
   */
  TailoringReference<T, TNamespace> addTailoringReference(
      TailoringReferenceType referenceType, T referenceTarget);

  /**
   * Adds a new {@link LinkTailoringReference} to this item. Use this method for {@link
   * TailoringReferenceType#LINK} & {@link TailoringReferenceType#LINK_EXTERNAL} only.
   */
  LinkTailoringReference<T, TNamespace> addLinkTailoringReference(
      TailoringReferenceType tailoringReferenceType,
      T target,
      String linkType,
      Map<String, Object> attributes);

  /**
   * Adds a new {@link RiskTailoringReference} to this item. Use this method for {@link
   * TailoringReferenceType#RISK} only.
   */
  RiskTailoringReference<T, TNamespace> addRiskTailoringReference(
      TailoringReferenceType referenceType,
      T target,
      @Nullable T riskOwner,
      @Nullable T mitigation,
      Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions);

  ControlImplementationTailoringReference<T, TNamespace> addControlImplementationReference(
      T control, @Nullable T responsible, @Nullable String description);

  RequirementImplementationTailoringReference<T, TNamespace> addRequirementImplementationReference(
      T control,
      @NotNull ImplementationStatus status,
      @Nullable String implementationStatement,
      @Nullable LocalDate implementationUntil,
      @Nullable T responsible,
      @Nullable Integer cost,
      @Nullable LocalDate implementationDate,
      @Nullable T implementedBy,
      @Nullable T document,
      @Nullable LocalDate lastRevisionDate,
      @Nullable T lastRevisionBy,
      @Nullable LocalDate nextRevisionDate,
      @Nullable T nextRevisionBy);

  default boolean isAppliedTo(Element element) {
    return findCatalogItem()
        .flatMap(
            item ->
                element
                    .findAppliedCatalogItem(requireDomainMembership())
                    .map(elementItem -> elementItem.equals(item)))
        .orElse(false);
  }

  Optional<CatalogItem> findCatalogItem();
}
