/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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

class V114__fix_null_pot_impacts extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        new Sql(context.connection).with {
            execute("""
UPDATE risk_definition_set
set risk_definitions = (select jsonb_object_agg(key, value || jsonb_build_object(
        'categories', (select array_agg(value || jsonb_build_object(
                'potentialImpacts', case when (value -> 'potentialImpacts' <> 'null'::jsonb) then value -> 'potentialImpacts' else '[]'::jsonb end))
                       from jsonb_array_elements(value -> 'categories'))))
                        from jsonb_each(risk_definitions))
where risk_definitions <> '{}'::jsonb;
        """)
        }
    }
}