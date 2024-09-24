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

class V97__add_mitigation_cis_and_ris extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""

/* Store missing CIs (mitigations where no CI exists yet) in temp table. */
create temporary table missing_cis
(
    risk_affected_id uuid not null,
    control_id       uuid not null
) on commit drop;
insert into missing_cis
select distinct r.entity_db_id, r.control_id
from abstractriskdata as r
         left join control_implementation as ci on ci.owner_db_id = r.entity_db_id and ci.control_id = r.control_id
where r.control_id is not null
  and ci.id is null;

/*
 Fill another temp table with the complete composite hierarchy for each control appearing in the temp missing CI
 table.
*/
create temporary table required_controls
(
    control_id      uuid not null,
    root_control_id uuid not null
) on commit drop;
with recursive control_hierarchy(control_id, root_control_id)
                   as (select mci.control_id, mci.control_id
                       from missing_cis as mci
                       group by mci.control_id
                       union
                       select cp.part_id, ch.root_control_id
                       from control_hierarchy as ch
                                inner join control_parts as cp on cp.composite_id = ch.control_id)
insert
into required_controls
select *
from control_hierarchy;

/*
 Store all required RIs for the missing CIs in another temp table. Some of these RIs may already exist in the DB,
 others have yet to be created and are assigned random UUIDs here.
*/
create temporary table required_ris
(
    risk_affected_id uuid not null,
    control_id       uuid not null,
    ri_id            uuid not null,
    ri_exists        bool not null
) on commit drop;
insert
into required_ris
select mci.risk_affected_id, rc.control_id, coalesce(ri.id, gen_random_uuid()), ri.id is not null
from missing_cis as mci
         left join required_controls as rc on rc.root_control_id = mci.control_id
         left join requirement_implementation as ri
                   on ri.origin_db_id = mci.risk_affected_id and ri.control_id = rc.control_id
group by mci.risk_affected_id, rc.control_id, ri.id;

/* Insert missing CIs */
insert into control_implementation
select nextval('seq_control_impl'), null, gen_random_uuid(), json_agg(rri.ri_id), mci.control_id, mci.risk_affected_id, null
from missing_cis as mci
         left join required_controls as rc on rc.root_control_id = mci.control_id
         left join required_ris rri on rri.control_id = rc.control_id and rri.risk_affected_id = mci.risk_affected_id
group by mci.control_id, mci.risk_affected_id;

/* Insert missing RIs */
insert into requirement_implementation
select nextval('seq_req_impl'),
       rri.ri_id,
       null,
       'SYSTEM_SPECIFIC',
       'UNKNOWN',
       rri.control_id,
       rri.risk_affected_id,
       null,
       null
from required_ris rri
where rri.ri_exists = false;

        """.toString())
        }
    }
}
