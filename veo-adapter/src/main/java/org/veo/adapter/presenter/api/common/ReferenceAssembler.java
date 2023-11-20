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
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.ref.ITypedId;

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
   * @param risk target risk
   * @return the URI of the specific target object
   */
  String targetReferenceOf(AbstractRisk<?, ?> risk);

  /**
   * Returns a reference to a collection of searches for the target type. Searches are a ressource
   * so they can be created, requested and deleted (think: "named search").
   *
   * @param type the {@link Identifiable} type
   * @return the URI of the resource collection for searches or {@code null} if the resource does
   *     not support searches.
   */
  String searchesReferenceOf(Class<? extends Identifiable> type);

  /**
   * Returns a reference to the resource collection of the target type.
   *
   * @param type the {@link Identifiable}
   * @return the URI of the resource collection or {@code null} if no resource collection for the
   *     type is exposed.
   */
  String resourcesReferenceOf(Class<? extends Identifiable> type);

  /**
   * Extract the objects type from the given URI.
   *
   * @param uri the URI which may be a URL.
   * @return The class of the specific {@link Identifiable} type.
   */
  Class<? extends Identifiable> parseType(String uri);

  /**
   * Extract the objects ID from the given URI.
   *
   * @param uri the URI which may be a URL
   */
  String parseId(String uri);

  /** Transforms the given adapter layer reference to an entity key. */
  Key<UUID> toKey(ITypedId<? extends Identifiable> reference);

  /** Transforms the given adapter layer references to entity keys. */
  Set<Key<UUID>> toKeys(Set<? extends ITypedId<?>> references);

  String schemaReferenceOf(String typeSingularTerm);

  String parseElementIdInDomain(String targetInDomainUri);

  Class<Element> parseElementTypeInDomain(String targetInDomainUri);

  String targetReferenceOf(RequirementImplementation requirementImplementation);

  String requirementImplementationsOf(ControlImplementation controlImplementation);

  String inspectionReferenceOf(String id, Domain domain);
}
