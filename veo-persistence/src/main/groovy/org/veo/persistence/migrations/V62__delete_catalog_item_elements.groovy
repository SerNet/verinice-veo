/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

class V62__delete_catalog_item_elements extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            delete from element_domains as ed
                using element as e
                    where ed.element_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;

            delete from subtype_aspect as a
                using element as e
                    where a.owner_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;
            delete from control_risk_values_aspect as a
                using element as e
                    where a.owner_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;
            delete from impact_values_aspect as a
                using element as e
                    where a.owner_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;
            delete from scenario_risk_values_aspect as a
                using element as e
                    where a.owner_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;
            delete from custom_aspect as a
                using element as e
                    where a.owner_db_id = e.db_id and
                          e.containing_catalog_item_id is not null;

            delete from element where containing_catalog_item_id is not null;

            alter table element
                drop column containing_catalog_item_id,
                alter column owner_id set not null;
            """)
        }
    }
}
