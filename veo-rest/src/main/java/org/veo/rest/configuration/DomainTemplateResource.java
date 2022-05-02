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
package org.veo.rest.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import org.veo.core.VeoInputStreamResource;
import org.veo.rest.common.SpringResourceWrapper;

@Component
public class DomainTemplateResource {

  @Value("${veo.domain.file.selector:classpath:domaintemplates/*.json}")
  private Resource[] springDomainResources;

  public List<VeoInputStreamResource> getResources() {
    return Arrays.stream(springDomainResources)
        .map(SpringResourceWrapper::new)
        .collect(Collectors.toList());
  }
}
