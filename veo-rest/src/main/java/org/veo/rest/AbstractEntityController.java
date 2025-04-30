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
package org.veo.rest;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.validation.annotation.Validated;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.service.EtagService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
public abstract class AbstractEntityController extends AbstractVeoController {

  @Autowired EntityToDtoTransformer entityToDtoTransformer;

  @Autowired ReferenceAssembler urlAssembler;

  @Autowired protected EtagService etagService;

  protected AbstractEntityController() {}

  protected CacheControl defaultCacheControl = CacheControl.noCache();

  protected <T extends Identifiable & Versioned> Optional<String> getEtag(
      Class<T> entityClass, UUID id) {
    return etagService.getEtag(entityClass, id);
  }
}
