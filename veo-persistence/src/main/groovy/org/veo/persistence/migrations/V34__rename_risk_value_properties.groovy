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

import java.sql.ResultSet

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import com.fasterxml.jackson.databind.ObjectMapper

import groovy.sql.Sql

class V34__rename_risk_value_properties extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        def om = new ObjectMapper()
        var con = new Sql(context.connection)
        con.query("select db_id, risk_categories from riskvalues_aspect") { ResultSet aspect ->
            while(aspect.next()) {
                def id = aspect.getString("db_id")
                def categories = aspect.getString("risk_categories").with{om.readValue(it, Collection.class)}
                categories.each{
                    it.userDefinedResidualRisk = it.residualRisk
                    it.residualRisk = it.effectiveRisk
                    it.remove("effectiveRisk")
                }
                con.execute("update riskvalues_aspect set risk_categories = jsonb(?::text) where db_id = ?", [
                    om.writeValueAsString(categories),
                    id
                ])
            }
        }
    }
}
