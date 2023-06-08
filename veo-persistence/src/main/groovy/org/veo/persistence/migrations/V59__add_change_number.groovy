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
                   add column change_number int8;
                alter table catalog
                   add column change_number int8;
                alter table catalogitem
                   add column change_number int8;
                alter table client
                   add column change_number int8;
                alter table domain
                   add column change_number int8;
                alter table domaintemplate
                   add column change_number int8;
                alter table element
                   add column change_number int8;
                alter table unit
                   add column change_number int8;

                UPDATE abstractriskdata SET change_number = version;
                UPDATE catalog SET change_number = version;
                UPDATE catalogitem SET change_number = version;
                UPDATE client SET change_number = version;
                UPDATE domain SET change_number = version;
                UPDATE domaintemplate SET change_number = version;
                UPDATE element SET change_number = version;
                UPDATE unit SET change_number = version;

                ALTER TABLE abstractriskdata
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE catalog
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE catalogitem
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE client
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE domain
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE domaintemplate
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE element
                    ALTER COLUMN change_number SET NOT NULL;
                ALTER TABLE unit
                    ALTER COLUMN change_number SET NOT NULL;
            """)
        }
    }
}
