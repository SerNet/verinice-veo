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

class V54__fix_aspect_domains extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            fixAspectDomainIds(it, "control_risk_values_aspect")
            fixAspectDomainIds(it, "process_impact_values_aspect")
            fixAspectDomainIds(it, "scenario_risk_values_aspect")
        }
    }

    /**
     * Fix domain reference on aspects by using domain ID from corresponding subtype aspect. Assumes that there are no
     * multi-domain elements yet.
     */
    private static boolean fixAspectDomainIds(Sql sql, String aspectTable) {
        // Verify that each element with this type of aspect has one domain.
        sql.query("""
                select *
                from (select e.db_id as element_id,
                             (select count(domain_id) from subtype_aspect as sta where sta.owner_db_id = e.db_id) as domain_count,
                             (select count(domain_id) from $aspectTable as a where a.owner_db_id = e.db_id) as a_count
                      from element as e) as query
                where query.a_count > 1 OR (query.a_count = 1 and query.domain_count != 1);
            """.toString()) {
                    def errors = []
                    while (it.next()) {
                        errors.add("Element ${it.getInt("element_id")} is associated with ${it.getInt("domain_count")} domains and has ${it.getInt("a_count")} $aspectTable aspects")
                    }
                    if (!errors.empty) {
                        throw new RuntimeException("All elements must have one $aspectTable at most, and all elements with an $aspectTable must be assigned to exactly one domain.\n\n${errors.join("\n")}")
                    }
                }

        sql.execute("""
                update ${aspectTable} as a
                set domain_id = sta.domain_id
                from subtype_aspect as sta
                where sta.owner_db_id = a.owner_db_id;
            """.toString())
    }
}