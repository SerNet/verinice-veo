/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity;

import java.util.Optional;

import javax.validation.constraints.NotNull;

/**
 * The domain should be referenced by the domain objects if applicable. It defines a standard, a
 * best practice or a company-specific context. It can be bound to a domain template, which
 * represent a 'offical' standart. The domain references the avaliable customAspects, the forms and
 * other stuff needed to define a specific view on the data. For example the risk definition, which
 * describes what risk values exist and how they relate to each other.
 */
public interface Domain extends DomainBase, ClientOwned {

  String SINGULAR_TERM = "domain";
  String PLURAL_TERM = "domains";

  boolean isActive();

  void setActive(boolean aActive);

  DomainTemplate getDomainTemplate();

  void setDomainTemplate(DomainTemplate aDomaintemplate);

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return Domain.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  void setOwner(Client owner);

  @NotNull
  Client getOwner();

  default Optional<Client> getOwningClient() {
    return Optional.of(getOwner());
  }
}
