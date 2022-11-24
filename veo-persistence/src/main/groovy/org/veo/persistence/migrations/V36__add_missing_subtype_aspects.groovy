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
import jakarta.transaction.NotSupportedException

class V36__add_missing_subtype_aspects extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        var con = new Sql(context.connection)
        // select all domain associations that have no corresponding sub type aspect, also fetching the element type
        con.eachRow("""
                select e.db_id as element_id, e.dtype as element_type, ed.domains_db_id as domain_id
                    from element as e
                    inner join element_domains as ed on ed.element_db_id = e.db_id
                    where (
                        select count(*)
                            from subtype_aspect as sa
                            where sa.owner_db_id = e.db_id and sa.domain_id = ed.domains_db_id
                        ) = 0;
            """) { GroovyResultSet association ->
                    // add a sub type aspect for each sub-type-less association
                    con.execute([
                        id: UUID.randomUUID().toString(),
                        elementId: association.getString("element_id"),
                        domainId: association.getString("domain_id"),
                        subType: getSubType(association.getString("element_type")),
                        status: "NEW",
                    ], "insert into subtype_aspect (db_id, owner_db_id, domain_id, sub_type, status) " +
                    "values (:id, :elementId, :domainId, :subType, :status)")
                }
    }

    static String getSubType(String type) {
        // This assumes all domains to be DS-GVO. I know, multi domain is just an illusion.
        switch (type) {
            case "asset": return "AST_Application"
            case "control": return "CTL_TOM"
            case "document": return "DOC_Document"
            case "incident": return "INC_Incident"
            case "person": return "PER_Person"
            case "process": return "PRO_DataTransfer"
            case "scenario": return "SCN_Scenario"
            case "scope": return "SCP_Scope"
            default: throw new NotSupportedException("Unknown element type '$type'")
        }
    }
}
