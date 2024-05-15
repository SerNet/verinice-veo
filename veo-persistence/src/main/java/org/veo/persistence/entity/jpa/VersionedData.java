/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.veo.core.entity.UnexpectedChangeNumberException;
import org.veo.core.entity.Versioned;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

/**
 * @author urszeidler
 */
@MappedSuperclass
@ToString(onlyExplicitlyIncluded = true)
@Data
@EntityListeners({AuditingEntityListener.class})
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod") // PMD does not see Lombok's methods
public abstract class VersionedData implements Versioned {

  @ToString.Include @Version private long version;

  @Column(name = "change_number", nullable = false)
  @Setter(AccessLevel.NONE)
  private long changeNumber;

  public synchronized long getChangeNumber() {
    return changeNumber;
  }

  @Column(name = "created_at", nullable = false)
  @CreatedDate
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @LastModifiedDate
  private Instant updatedAt;

  @Column(name = "created_by", nullable = false)
  @CreatedBy
  private String createdBy;

  @Column(name = "updated_by", nullable = false)
  @LastModifiedBy
  private String updatedBy;

  @Override
  public synchronized long nextChangeNumberForUpdate() {
    changeNumber++;
    return changeNumber;
  }

  @Override
  public synchronized long initialChangeNumberForInsert() {
    if (changeNumber != 0) {
      throw new UnexpectedChangeNumberException(
          "Cannot insert with change number 0, already at %d on type %s."
              .formatted(getChangeNumber(), getClass().getSimpleName()));
    }
    return 0;
  }

  @Override
  public synchronized void consolidateChangeNumber(long lowestSeenChangeNo) {
    changeNumber = lowestSeenChangeNo;
  }
}
