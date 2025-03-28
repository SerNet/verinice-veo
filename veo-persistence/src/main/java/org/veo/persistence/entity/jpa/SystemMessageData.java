/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.TranslatedText;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@Entity(name = "system_message")
@EntityListeners(AuditingEntityListener.class)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class SystemMessageData implements SystemMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_system_messages")
  @SequenceGenerator(name = "seq_system_messages", allocationSize = 1)
  @ToString.Include
  private Long id;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private TranslatedText message = TranslatedText.empty();

  @Column(name = "created_at", nullable = false)
  @CreatedDate
  @NotNull
  @ToString.Include
  private Instant createdAt;

  @NotNull @ToString.Include private Instant publication;
  @Nullable @ToString.Include private Instant effective;

  private MessageLevel level;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof SystemMessageData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
