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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import org.veo.core.entity.Client;
import org.veo.core.entity.UserConfiguration;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.ToString;

@Entity(name = "userconfiguration")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class UserConfigurationData implements UserConfiguration {
  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @ToString.Include
  private String dbId;

  @ToString.Include private String userName;
  @ToString.Include private String applicationId;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ClientData.class)
  @JoinColumn(name = "client_id", foreignKey = @ForeignKey(name = "FK_client_id"))
  private Client client;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> configuration = new HashMap<>();

  @Override
  public boolean isPersisted() {
    return getDbId() != null;
  }

  @Override
  public Optional<Client> getOwningClient() {
    return Optional.ofNullable(client);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof UserConfigurationData)) return false;

    UserConfigurationData other = (UserConfigurationData) o;
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
}
