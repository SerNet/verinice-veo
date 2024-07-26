/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

class V92__more_indexes extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
                create index idx_abstractriskdata_control_id on abstractriskdata(control_id);
                create index idx_abstractriskdata_person_id on abstractriskdata(person_id);
                create index idx_abstractriskdata_scenario_db_id on abstractriskdata(scenario_db_id);

                create index idx_control_implementation_control_id on control_implementation(control_id);
                create index idx_control_implementation_person_id on control_implementation(person_id);

                create index idx_requirement_implementation_control_id on requirement_implementation(control_id);
                create index idx_requirement_implementation_person_id on requirement_implementation(person_id);
        ''')
        }
    }
}
