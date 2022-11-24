/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import org.veo.core.entity.profile.ProfileDefinition;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@Entity(name = "profile_set")
public class ProfileSetData {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_profile_sets")
  @SequenceGenerator(name = "seq_profile_sets")
  private Long id;

  @NotNull
  @Column(columnDefinition = "jsonb")
  @Valid
  @Type(JsonType.class)
  private Map<String, ProfileDefinition> profiles = new HashMap<>();

  public void setProfiles(Map<String, ProfileDefinition> profiles) {
    this.profiles.clear();
    this.profiles.putAll(profiles);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (!(o instanceof ProfileSetData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore, a transient entity must never equal another entity.
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
