/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.rest.common;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

import org.veo.core.VeoInputStreamResource;

import lombok.Value;

@Value
public class SpringResourceWrapper implements VeoInputStreamResource {
  Resource resource;

  @Override
  public InputStream getInputStream() throws IOException {
    return resource.getInputStream();
  }

  @Override
  public String getDescription() {
    return resource.getDescription();
  }
}
