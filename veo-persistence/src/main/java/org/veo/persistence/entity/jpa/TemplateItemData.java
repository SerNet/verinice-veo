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

import org.hibernate.annotations.Type;

import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.Unit;
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

  @Deprecated // TODO #2301 remove
  @ToString.Include
  @Column(name = "namespace")
  protected String namespace;

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
