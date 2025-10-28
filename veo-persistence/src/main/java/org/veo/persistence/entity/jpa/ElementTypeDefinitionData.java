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
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslationMap;
import org.veo.core.entity.definitions.ControlImplementationDefinition;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;

import lombok.Data;
import lombok.ToString;

@Entity(name = "element_type_definition")
@Data()
@EntityListeners({ElementTypeDefintionEntityListener.class})
public class ElementTypeDefinitionData implements ElementTypeDefinition {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "db_id")
  private UUID id;

  @Enumerated
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @NotNull
  private ElementType elementType;

  @ManyToOne(targetEntity = DomainBaseData.class, optional = false, fetch = FetchType.LAZY)
  @NotNull
  @Valid
  private DomainBase owner;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid
  private Map<String, SubTypeDefinition> subTypes = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid
  private Map<String, CustomAspectDefinition> customAspects = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid
  private Map<String, LinkDefinition> links = new HashMap<>();

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid
  private TranslationMap translations = new TranslationMap();

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Valid
  private ControlImplementationDefinition controlImplementationDefinition;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof ElementTypeDefinitionData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    UUID dbId = getId();
    return dbId != null && dbId.equals(other.getId());
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
