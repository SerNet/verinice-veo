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

class V71__fix_tailoring_reference_names extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""
            alter table tailoringreference
                rename to catalog_tailoring_reference;

            alter table catalog_tailoring_reference
                rename column catalog_item_db_id to target_db_id;
            alter table catalog_tailoring_reference
                rename column referencetype to reference_type;

            alter table profile_tailoring_reference
                rename column catalog_item_db_id to target_db_id;
            alter table profile_tailoring_reference
                rename column referencetype to reference_type;

            alter table updatereference
                rename column catalog_item_db_id to target_db_id;
            alter table updatereference
                rename column updatetype to update_type;
""")
    }
}
