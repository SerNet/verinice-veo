/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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
package org.veo.persistence.access;

import java.util.List;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.repository.CompositeElementQuery;
import org.veo.core.repository.ElementQuery;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.entity.jpa.ElementData;

public class CompositeElementQueryImpl<TInterface extends Element, TDataClass extends ElementData>
    extends ElementQueryImpl<TInterface, TDataClass> implements CompositeElementQuery<TInterface> {

  private final CompositeEntityDataRepository<TDataClass> compositeElementRepository;
  private boolean fetchPartsAndCompositesAndCompositeParts;

  public CompositeElementQueryImpl(
      CompositeEntityDataRepository<TDataClass> compositeElementRepository, Client client) {
    super(compositeElementRepository, client);
    this.compositeElementRepository = compositeElementRepository;
  }

  @Override
  public CompositeElementQueryImpl<TInterface, TDataClass>
      fetchPartsAndCompositesAndCompositesParts() {
    fetchPartsAndCompositesAndCompositeParts = true;
    return this;
  }

  @Override
  public ElementQuery<TInterface> fetchParentsAndChildrenAndSiblings() {
    super.fetchParentsAndChildrenAndSiblings();
    return fetchPartsAndCompositesAndCompositesParts();
  }

  @Override
  protected List<TDataClass> fullyLoadItems(List<String> ids) {
    List<TDataClass> result = super.fullyLoadItems(ids);
    if (fetchPartsAndCompositesAndCompositeParts) {
      compositeElementRepository.findAllWithPartsAndCompositesAndCompositesPartsByDbIdIn(ids);
    }
    return result;
  }
}
