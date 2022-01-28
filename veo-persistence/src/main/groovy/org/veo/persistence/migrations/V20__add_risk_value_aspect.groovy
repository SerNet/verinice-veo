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

class V20__add_risk_value_aspect extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    create table riskvalue_aspect (
        db_id varchar(255) not null,
        impact_categories jsonb,
        probability jsonb,
        risk_categories jsonb,
        risk_definition varchar(255),
        domain_id varchar(255) not null,
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

    alter table riskvalue_aspect
       add constraint FK_owner_db_id
       foreign key (owner_db_id)
       references abstractriskdata;

""")
    }
}