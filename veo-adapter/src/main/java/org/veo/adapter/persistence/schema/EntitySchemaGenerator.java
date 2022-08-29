/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.adapter.persistence.schema;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.veo.core.entity.Domain;
import org.veo.core.entity.EntitySchemaException;

public class EntitySchemaGenerator {

  private final SchemaExtender schemaExtender;

  private final ObjectWriter writer;

  public EntitySchemaGenerator(SchemaExtender schemaExtender) {
    this.schemaExtender = schemaExtender;

    CustomPrettyPrinter prettyPrinter = new CustomPrettyPrinter();
    prettyPrinter.indentArraysWith(new DefaultIndenter());
    writer = new ObjectMapper().writer(prettyPrinter);
  }

  public String createSchema(String baseName, Set<Domain> domains) {
    try {
      ObjectNode jsonSchema = SchemaProvider.getInstance().getSchema(baseName);
      schemaExtender.extendSchema(jsonSchema, baseName, domains);

      return writer.writeValueAsString(jsonSchema);
    } catch (JsonProcessingException e) {
      throw new EntitySchemaException("Schema creation failed", e);
    }
  }
}
