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

class V78__add_potential_impact_props extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""
    with i as (select db_id as id,
           (select jsonb_object_agg(key, value || jsonb_build_object(
                   'potentialImpactsEffective', value -> 'potentialImpacts',
                   'potentialImpactReasons',
                   (select jsonb_object_agg(key, 'impact_reason_manual') from jsonb_each(value -> 'potentialImpacts')),
                   'potentialImpactEffectiveReasons',
                   (select jsonb_object_agg(key, 'impact_reason_manual') from jsonb_each(value -> 'potentialImpacts')),
                   'potentialImpactsCalculated', '{}'::jsonb,
                   'potentialImpactExplanations', '{}'::jsonb
            ))
            from jsonb_each(impact_values)) as migrated_impact_values
        from impact_values_aspect
    )
    update impact_values_aspect
    set impact_values = i.migrated_impact_values
    from i
    where impact_values != '{}'::jsonb and db_id = i.id;
 """)
    }
}
