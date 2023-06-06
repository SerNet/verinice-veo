/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import groovy.sql.Sql

class V59__add_change_number extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
                alter table abstractriskdata
                   add column change_number int8 not null;

                alter table catalog
                   add column change_number int8 not null;

                alter table catalogitem
                   add column change_number int8 not null;

                alter table client
                   add column change_number int8 not null;

                alter table domain
                   add column change_number int8 not null;

                alter table domaintemplate
                   add column change_number int8 not null;

                alter table element
                   add column change_number int8 not null;

                alter table unit
                   add column change_number int8 not null;
            """)
        }
    }
}
