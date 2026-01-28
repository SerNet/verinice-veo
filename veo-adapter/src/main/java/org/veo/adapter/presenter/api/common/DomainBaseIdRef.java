/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.adapter.presenter.api.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.NameAbbreviationAndDescription;
import org.veo.core.entity.Translated;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.TypedId;

import lombok.NonNull;

public class DomainBaseIdRef<TDomain extends DomainBase> extends IdRef<TDomain> {
  protected DomainBaseIdRef(
      ITypedId<TDomain> ref,
      String displayName,
      ReferenceAssembler urlAssembler,
      String uri,
      TDomain entity) {
    super(ref, displayName, urlAssembler, uri, entity);
  }

  public static <T extends DomainBase> DomainBaseIdRef<T> from(
      T entity, @NonNull ReferenceAssembler urlAssembler) {
    if (entity == null) return null;
    return new DomainBaseIdRef<>(
        TypedId.from(entity), entity.getDisplayName(), urlAssembler, null, entity);
  }

  public static DomainBaseIdRef<?> fromTargetUri(String uri, ReferenceAssembler urlAssembler) {
    TypedId<?> typedId = urlAssembler.parseIdentifiableRef(uri);
    if (DomainBase.class.isAssignableFrom(typedId.getType())) {
      return new DomainBaseIdRef<>((ITypedId<DomainBase>) typedId, null, urlAssembler, uri, null);
    }
    throw new IllegalArgumentException(
        "URI '%s' does not reference a domain or domain template".formatted(uri));
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Translated<NameAbbreviationAndDescription> getTranslations() {
    return entity.getTranslations();
  }

  // TODO #4546 throw away
  @JsonIgnore
  @Override
  public String getDesignator() {
    return super.getDesignator();
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getTemplateVersion() {
    return entity.getTemplateVersion();
  }
}
