/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
package org.veo.adapter.service.domaintemplate;

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;

import org.veo.core.service.DomainTemplateIdGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DomainTemplateIdGeneratorImpl implements DomainTemplateIdGenerator {

  private static final String SERNET_VERNDOR_URL = "https://v.de/veo/domain-templates/";

  @Override
  public String createDomainTemplateId(String name, String version, String revision) {
    String url = SERNET_VERNDOR_URL + name + "/" + version + "." + revision;
    UUID namebaseUUID =
        Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL).generate(url);
    log.info("generated domain template id url:{} UUID:{}", url, namebaseUUID);
    return namebaseUUID.toString();
  }
}
