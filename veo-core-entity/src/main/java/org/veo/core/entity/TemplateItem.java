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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.TailoringReferenceState;
import org.veo.core.entity.state.TemplateItemState;

public interface TemplateItem<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TemplateItemState<T, TNamespace>, SymIdentifiable<T, TNamespace>, Versioned {
  void setElementType(String aType);

  void setSubType(String subType);

  void setStatus(String status);

  Map<String, Map<String, Object>> getCustomAspects();

  @Override
  default UUID getSelfId() {
    return getSymbolicId().value();
  }

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
    return (Class<? extends Element>) EntityType.getBySingularTerm(getElementType()).getType();
  }

  static void checkValidElementType(String type) {
    if (EntityType.ELEMENT_TYPES.stream().noneMatch(et -> et.getSingularTerm().equals(type))) {
      throw new UnprocessableDataException(
          "The given elementType '"
              + type
              + "' is not a valid template type. Valid types are: "
              + EntityType.ELEMENT_TYPES.stream()
                  .map(et -> et.getSingularTerm())
                  .collect(Collectors.joining(", ")));
    }
  }

  DomainBase getDomainBase();

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
}
