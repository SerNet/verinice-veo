/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.aspects.SubTypeAspect;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "subtype_aspect")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubTypeAspectData extends AspectData implements SubTypeAspect {
  public SubTypeAspectData(DomainBase domain, Element owner, String subType, String status) {
    super(domain, owner);
    this.subType = subType;
    this.status = status;
  }

  @NotNull @Getter @ToString.Include private String subType;

  @NotNull @Getter @Setter @ToString.Include private String status;
}
