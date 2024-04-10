/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

class V83__add_symbolic_template_item_ids extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
                alter table catalogitem
                   add column symbolic_db_id varchar(255);
                alter table profile_item
                   add column symbolic_db_id varchar(255);

                update catalogitem
                    set symbolic_db_id = db_id;
                update profile_item
                    set symbolic_db_id = db_id;

                alter table catalogitem
                    alter column symbolic_db_id set not null;
                alter table profile_item
                    alter column symbolic_db_id set not null;
        """.toString())
        }
    }
}
