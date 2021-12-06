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

import groovy.sql.Sql

class V9__add_generic_status extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {

        new Sql(context.connection).execute("""

            alter table aspect
                add column status varchar(255);

            update aspect as a
                set status = e.status
                from element as e
                where e.db_id = a.owner_db_id;

            alter table element
                drop column status;

            update aspect set status = 'NEW' where sub_type = 'AST_Application';
            update aspect set status = 'NEW' where sub_type = 'AST_Datatype';
            update aspect set status = 'NEW' where sub_type = 'AST_IT-System';

            update aspect set status = 'NEW' where sub_type = 'CTL_TOM';

            update aspect set status = 'NEW' where sub_type = 'DOC_Document';

            update aspect set status = 'NEW' where sub_type = 'PER_DataProtectionOfficer';
            update aspect set status = 'NEW' where sub_type = 'PER_Person';

            update aspect set status = 'NEW' where sub_type = 'SCN_Scenario';

            update aspect set status = 'NEW' where sub_type = 'SCP_Controller';
            update aspect set status = 'NEW' where sub_type = 'SCP_JointController';
            update aspect set status = 'NEW' where sub_type = 'SCP_Processor';
            update aspect set status = 'NEW' where sub_type = 'SCP_ResponsibleBody';
""")
    }
}
