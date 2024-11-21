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

class V101__remove_legacy_impl_status extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            it.execute("""
with legacy_impl as (select c.db_id                                                                 as control_id,
                            c.name,
                            (ca.attributes ->> 'control_implementation_date')::date                 as date,
                            ca.attributes ->> 'control_implementation_explanation'                  as explanation,
                            (crv.control_risk_values -> 'DSRA' ->> 'implementationStatus')::integer as status
                     from element as c
                              left join element_domain_association as eda on eda.owner_db_id = c.db_id
                              inner join domain as d on d.db_id = eda.domain_id and d.name = 'DS-GVO'
                              left join custom_aspect as ca
                                        on ca.owner_db_id = c.db_id and ca.domain_id = d.db_id and ca.type = 'control_implementation'
                              left join control_risk_values_aspect as crv on crv.owner_db_id = c.db_id and crv.domain_id = d.db_id
                     where ca.db_id is not null
                        or crv.db_id is not null)
update requirement_implementation as ri
set status = CASE
                                   WHEN ri.status != 'UNKNOWN' THEN ri.status
                                   WHEN l.status = 0 THEN 'YES'
                                   WHEN l.status = 1 THEN 'NO'
                                   WHEN l.status = 2 THEN 'PARTIAL'
                                   WHEN l.status = 3 THEN 'N_A'
                                   ELSE 'UNKNOWN'
    END,
    implementation_statement = coalesce(ri.implementation_statement, NULLIF(l.explanation, '')),
    implementation_until = coalesce(ri.implementation_until, l.date)

from legacy_impl as l
where ri.control_id = l.control_id;

update catalogitem set aspects = aspects - 'controlRiskValues' where aspects -> 'controlRiskValues' is not null;
update profile_item set aspects = aspects - 'controlRiskValues' where aspects -> 'controlRiskValues' is not null;

drop table control_risk_values_aspect;
        """)
        }
    }
}