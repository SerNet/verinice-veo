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

class V55__assign_custom_aspects_to_a_domain extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            // Verify that every element with custom aspects / links is associated with one domain
            query("""
                select *
                from (select e.db_id as element_id,
                             (select count(domain_id) from subtype_aspect as sta where sta.owner_db_id = e.db_id) as domain_count,
                             (select count(db_id) from custom_aspect as ca where e.db_id in (ca.owner_db_id, ca.source_id)) as ca_count
                      from element as e) as query
                where query.ca_count > 0 and query.domain_count != 1;
            """) {
                        def errors = []
                        while (it.next()) {
                            errors.add("Element ${it.getString("element_id")} has ${it.getInt("ca_count")} custom aspects / links, but is associated with ${it.getInt("domain_count")} domains")
                        }
                        if (!errors.empty) {
                            throw new RuntimeException("This migration requires all elements with custom aspects / links to be associated with exactly one domain\n\n${errors.join("\n")}")
                        }
                    }

            // Assign every custom aspect / link to the domain that its owner / source is associated with
            execute("""
                drop table custom_aspect_domains;
                alter table custom_aspect
                    add column domain_id varchar(36);

                update custom_aspect as ca
                set domain_id = sta.domain_id
                from subtype_aspect as sta
                where sta.owner_db_id in (ca.owner_db_id, ca.source_id);

                alter table custom_aspect
                    alter column domain_id set not null;
            """)
        }
    }
}
