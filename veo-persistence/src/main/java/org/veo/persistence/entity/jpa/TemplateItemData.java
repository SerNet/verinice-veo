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
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
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
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@MappedSuperclass
public abstract class TemplateItemData<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends VersionedData implements TemplateItem<T, TNamespace> {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  private UUID dbId;

  @NotNull @ToString.Include private UUID symbolicDbId;

  @Override
  public Key<UUID> getSymbolicId() {
    return Optional.ofNullable(symbolicDbId).map(Key::from).orElse(null);
  }

  @Override
  public String getSymbolicIdAsString() {
    return symbolicDbId.toString();
  }

  @Override
  public void setSymbolicId(Key<UUID> symbolicId) {
    symbolicDbId = symbolicId.value();
  }

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
  public TailoringReference<T, TNamespace> addTailoringReference(
      TailoringReferenceType referenceType, T target) {
    var ref = createTailoringReference();
    add(ref, referenceType, target);
    return ref;
  }

  @Override
  public LinkTailoringReference<T, TNamespace> addLinkTailoringReference(
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
  public RiskTailoringReference<T, TNamespace> addRiskTailoringReference(
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
  public ControlImplementationTailoringReference<T, TNamespace> addControlImplementationReference(
      T control, @Nullable T responsible, @Nullable String description) {
    var ref = createControlImplementationTailoringReference();
    add(ref, TailoringReferenceType.CONTROL_IMPLEMENTATION, control);
    ref.setResponsible(responsible);
    ref.setDescription(description);
    return ref;
  }

  protected void add(
      TailoringReference<T, TNamespace> reference, TailoringReferenceType type, T target) {
    reference.setReferenceType(type);
    reference.setOwner((T) this);
    reference.setTarget(target);
  }

  protected abstract TailoringReference<T, TNamespace> createTailoringReference();

  protected abstract LinkTailoringReference<T, TNamespace> createLinkTailoringReference();

  protected abstract RiskTailoringReference<T, TNamespace> createRiskTailoringReference();

  protected abstract ControlImplementationTailoringReference<T, TNamespace>
      createControlImplementationTailoringReference();

  protected Element createElement(Unit owner) {
    TemplateItem.checkValidElementType(getElementType());
    var element =
        new IdentifiableDataFactory()
            .create((Class<Element>) EntityType.getBySingularTerm(getElementType()).getType());
    element.setOwner(owner);
    return element;
  }

  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (o instanceof TemplateItemData<?, ?> other) {
      // Transient (unmanaged) entities have an ID of 'null'. Only managed
      // (persisted and detached) entities have an identity. JPA requires that
      // an entity's identity remains the same over all state changes.
      // Therefore, a transient entity must never equal another entity.
      UUID dbId = getDbId();
      return dbId != null && dbId.equals(other.getDbId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
