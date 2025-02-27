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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.adapter.presenter.api.dto.full.FullProfileItemDto;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.ProfileItemState;
import org.veo.core.entity.state.TailoringReferenceState;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExportProfileItemDto extends FullProfileItemDto implements ProfileItemState {
  private SymIdRef<CatalogItem, DomainBase> appliedCatalogItem;

  @Nullable
  @Override
  @JsonIgnore
  public ITypedSymbolicId<CatalogItem, DomainBase> getAppliedCatalogItemRef() {
    return appliedCatalogItem;
  }

  @Override
  @JsonIgnore
  public Set<CustomAspectState> getCustomAspectStates() {
    return getCustomAspects().getValue().entrySet().stream()
        .map(
            kv ->
                new CustomAspectState.CustomAspectStateImpl(kv.getKey(), kv.getValue().getValue()))
        .collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public Set<TailoringReferenceState<ProfileItem, Profile>> getTailoringReferenceStates() {
    return getTailoringReferences().stream()
        .map(tr -> (TailoringReferenceState<ProfileItem, Profile>) tr)
        .collect(Collectors.toSet());
  }
}
