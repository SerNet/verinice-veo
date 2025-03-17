/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.CompoundIdentifiable;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.SymIdentifiable;
import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedId;

/**
 * Construct and deconstruct references to {@link Identifiable} & {@link AbstractRisk} objects.
 *
 * <p>These may be HTTP-URLs in a REST application with the correct hostname based on the received
 * request.
 */
public interface ReferenceAssembler {

  /**
   * Returns an absolute reference to the target object of this reference.
   *
   * @param identifiable the {@link Identifiable} object
   * @return the URI of the specific target object
   */
  String targetReferenceOf(Identifiable identifiable);

  /**
   * Returns an absolute reference to the target element from the viewpoint of the target domain.
   */
  String elementInDomainRefOf(Element element, Domain domain);

  /**
   * Returns an absolute reference to the target object, where the target object is identified using
   * a compound key.
   *
   * <p>I.e. the URI to an object with a compound ID could look like this
   * "/risks/<ASSET-UUID>/<SCENARIO-UUID>"
   *
   * @param entity target entity
   * @return the URI of the specific target object
   */
  String targetReferenceOf(CompoundIdentifiable<?, ?> entity);

  /**
   * Returns an absolute reference to the target object, where the target object is identified using
   * a symbolic ID and namespace (parent entity) reference.
   *
   * <p>I.e. the URI to an object with a symbolic ID could look like this:
   * "/domains/<DOMAIN-UUID>/catalog-items/<CATALOG-ITEM-SYMBOLIC-UUID>"
   *
   * @return the URI of the specific target object
   */
  <T extends SymIdentifiable<T, TNamespace>, TNamespace extends Identifiable>
      String targetReferenceOf(T entity);

  String targetReferenceOf(UserConfiguration userConfiguration);

  String targetReferenceOf(SystemMessage systemMessage);

  TypedId<?> parseIdentifiableRef(String url);

  TypedId<? extends Element> parseElementRef(String url);

  /** Transforms the given adapter layer reference to an entity key. */
  UUID toKey(ITypedId<? extends Identifiable> reference);

  /** Transforms the given adapter layer references to entity keys. */
  Set<UUID> toKeys(Set<? extends ITypedId<?>> references);

  String targetReferenceOf(RequirementImplementation requirementImplementation);

  String requirementImplementationsOf(ControlImplementation controlImplementation);

  String inspectionReferenceOf(String id, Domain domain);

  ITypedSymbolicId<?, ?> parseSymIdentifiableUri(String uri);
}
