/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.persistence.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V11__add_element_type_definitions extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {

        context.getConnection().createStatement().execute("""

            create table domaintemplate_element_type_definitions (
               domaintemplate_db_id varchar(255) not null,
                element_type_definitions_db_id varchar(255) not null,
                primary key (domaintemplate_db_id, element_type_definitions_db_id)
            );

            create table element_type_definition (
               db_id varchar(255) not null,
                custom_aspects jsonb not null,
                element_type varchar(255) not null,
                links jsonb not null,
                sub_types jsonb,
                owner_db_id varchar(255) not null,
                primary key (db_id)
            );

            alter table domaintemplate_element_type_definitions
               add constraint UK_element_type_definitions_db_id unique (element_type_definitions_db_id);

            alter table domaintemplate_element_type_definitions
               add constraint FK_type_definitions_db_id
               foreign key (element_type_definitions_db_id)
               references element_type_definition;

""")
    }
}
