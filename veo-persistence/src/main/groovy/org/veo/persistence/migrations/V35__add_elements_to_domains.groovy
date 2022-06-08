/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

import groovy.sql.GroovyResultSet
import groovy.sql.Sql

class V35__add_elements_to_domains extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        var con = new Sql(context.connection)
        // select each element that is in a unit and has no domain associations. also fetch one of its client's domains
        // (doesn't matter which domain in prod because there should only be one active domain per client)
        con.eachRow("""
                select e.db_id as element_id, (
                        select d.db_id from domain as d
                            inner join unit as u on u.client_id = d.owner_db_id
                            where d.active and u.db_id = e.owner_id
                            order by d.created_at desc
                            limit 1)
                    as first_active_client_domain_id from element as e
                where e.owner_id is not null
                    and (select count(*) from element_domains as ed where ed.element_db_id = e.db_id) = 0;
            """) { GroovyResultSet element ->
                    // associate domain-less element with one of its client's domains
                    con.execute([
                        elementId: element.getString("element_id"),
                        domainId: element.getString("first_active_client_domain_id")
                    ], "insert into element_domains (element_db_id, domains_db_id) values (:elementId, :domainId)")
                }
    }
}
