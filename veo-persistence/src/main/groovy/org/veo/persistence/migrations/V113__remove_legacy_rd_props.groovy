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

class V113__remove_legacy_rd_props extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        new Sql(context.connection).with {
            execute("""
UPDATE risk_definition_set
set risk_definitions = (select jsonb_object_agg(key, value || jsonb_build_object(
        'riskMethod', (value -> 'riskMethod') - 'impactMethod' - 'description',
        'riskValues',
        COALESCE((select jsonb_agg(value - 'name' - 'description' - 'abbreviation') from jsonb_array_elements(value -> 'riskValues')),
                 '[]'::jsonb),
        'categories', (select array_agg(value - 'name' - 'description' - 'abbreviation' || jsonb_build_object(
                'potentialImpacts',
                (select array_agg(value - 'name' - 'description' - 'abbreviation') from jsonb_array_elements(value -> 'potentialImpacts')),
                'valueMatrix', case
                                   when (value -> 'valueMatrix' = 'null'::jsonb) then null
                                   else (select array_agg((select array_agg(value - 'name' - 'description' - 'abbreviation')
                                                           from jsonb_array_elements(value)))
                                         from jsonb_array_elements(value -> 'valueMatrix')) end))
                       from jsonb_array_elements(value -> 'categories')),
        'probability',
        (value -> 'probability') - 'name' - 'description' - 'abbreviation' || jsonb_build_object(
                'levels', COALESCE((select jsonb_agg(value - 'name' - 'description' - 'abbreviation')
                                    from jsonb_array_elements(value -> 'implementationStateDefinition' -> 'levels')), '[]'::jsonb)),
        'implementationStateDefinition',
        (value -> 'implementationStateDefinition') - 'name' - 'description' - 'abbreviation' || jsonb_build_object(
                'levels', (select array_agg(value - 'name' - 'description' - 'abbreviation')
                           from jsonb_array_elements(value -> 'implementationStateDefinition' -> 'levels'))
                                                                                                )))
                        from jsonb_each(risk_definitions))
where risk_definitions <> '{}'::jsonb;
        """)
        }
    }
}