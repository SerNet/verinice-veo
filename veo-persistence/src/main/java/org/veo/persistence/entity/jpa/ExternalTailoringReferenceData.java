/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.ExternalTailoringReference;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(name = "externaltailoringreference")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ExternalTailoringReferenceData extends TailoringReferenceData
        implements ExternalTailoringReference {

    @OneToOne(targetEntity = CustomLinkDescriptorData.class,
              orphanRemoval = true,
              cascade = CascadeType.ALL)
    private CustomLink externalLink;

}
