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
package org.veo.persistence.entity.jpa;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.hibernate.annotations.Type;

import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.Unit;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.persistence.entity.jpa.transformer.IdentifiableDataFactory;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@MappedSuperclass
public abstract class TemplateItemData<T extends TemplateItem<T>> extends IdentifiableVersionedData
    implements TemplateItem<T> {
  @NotNull
  @Column(name = "name")
  @ToString.Include
  protected String name;

  @Column(name = "abbreviation")
  protected String abbreviation;

  @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
  protected String description;

  @NotNull
  @Column(name = "elementtype")
  protected String elementType;

  @NotNull
  @Column(name = "subtype")
  protected String subType;

  @NotNull
  @Column(name = "status")
  protected String status;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Map<String, Object>> customAspects = new HashMap<>();

  @NotNull
  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private TemplateItemAspects aspects = new TemplateItemAspects();

  public void setElementType(String elementType) {
    TemplateItem.checkValidElementType(elementType);
    this.elementType = elementType;
  }

  @Override
  public void clearTailoringReferences() {
    getTailoringReferences().forEach(tr -> tr.setOwner(null));
    getTailoringReferences().clear();
  }

  @Override
  public TailoringReference<T> addTailoringReference(
      TailoringReferenceType referenceType, T target) {
    var ref = createTailoringReference();
    add(ref, referenceType, target);
    return ref;
  }

  @Override
  public LinkTailoringReference<T> addLinkTailoringReference(
      TailoringReferenceType tailoringReferenceType,
      T target,
      String linkType,
      Map<String, Object> attributes) {
    var ref = createLinkTailoringReference();
    add(ref, tailoringReferenceType, target);
    ref.setLinkType(linkType);
    ref.setAttributes(attributes);
    return ref;
  }

  @Override
  public RiskTailoringReference<T> addRiskTailoringReference(
      TailoringReferenceType referenceType,
      T target,
      @Nullable T riskOwner,
      @Nullable T mitigation,
      Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions) {
    var ref = createRiskTailoringReference();
    add(ref, referenceType, target);
    ref.setRiskOwner(riskOwner);
    ref.setMitigation(mitigation);
    ref.setRiskDefinitions(riskDefinitions);
    return ref;
  }

  @Override
  public ControlImplementationTailoringReference<T> addControlImplementationReference(
      T control, @Nullable T responsible, @Nullable String description) {
    var ref = createControlImplementationTailoringReference();
    add(ref, TailoringReferenceType.CONTROL_IMPLEMENTATION, control);
    ref.setResponsible(responsible);
    ref.setDescription(description);
    return ref;
  }

  protected void add(TailoringReference<T> reference, TailoringReferenceType type, T target) {
    reference.setReferenceType(type);
    reference.setOwner((T) this);
    reference.setTarget(target);
  }

  protected abstract TailoringReference<T> createTailoringReference();

  protected abstract LinkTailoringReference<T> createLinkTailoringReference();

  protected abstract RiskTailoringReference<T> createRiskTailoringReference();

  protected abstract ControlImplementationTailoringReference<T>
      createControlImplementationTailoringReference();

  protected Element createElement(Unit owner) {
    TemplateItem.checkValidElementType(getElementType());
    var element =
        new IdentifiableDataFactory()
            .create(
                (Class<Element>) EntityType.getBySingularTerm(getElementType()).getType(), null);
    element.setOwner(owner);
    return element;
  }
}
