/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.TranslationMap;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;

import lombok.Data;
import lombok.ToString;

@Entity(name = "element_type_definition")
@Data()
@TypeDef(name = "json", typeClass = JsonType.class, defaultForType = Map.class)
@EntityListeners({ElementTypeDefintionEntityListener.class})
public class ElementTypeDefinitionData implements ElementTypeDefinition {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @NotNull private String elementType;

  @ManyToOne(targetEntity = DomainBaseData.class, optional = false, fetch = FetchType.LAZY)
  @NotNull
  @Valid
  private DomainBase owner;

  @Column(columnDefinition = "jsonb")
  private Map<String, SubTypeDefinition> subTypes = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  private Map<String, CustomAspectDefinition> customAspects = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  private Map<String, LinkDefinition> links = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  private TranslationMap translations = new TranslationMap();

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof ElementTypeDefinitionData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    String dbId = getDbId();
    return dbId != null && dbId.equals(other.getDbId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public Map<Locale, Map<String, String>> getTranslations() {
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Map<Locale, Map<String, String>> translations) {
    this.translations.setTranslations(translations);
  }
}
