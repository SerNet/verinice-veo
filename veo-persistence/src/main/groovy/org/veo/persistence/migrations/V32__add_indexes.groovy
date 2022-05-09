/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade
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

class V32__add_indexes extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

         create index IDX_CUSTOM_ASPECT_IDX_OWNER_DB_ID on custom_aspect(owner_db_id);

         create index IDX_CUSTOM_ASPECT_IDX_DTYPE on custom_aspect(dtype);

         create index IDX_CUSTOM_ASPECT_IDX_SOURCE_ID on custom_aspect(source_id);

         create index IDX_SUBTYPE_ASPECT_OWNER_DB_ID on subtype_aspect(owner_db_id);

         create index IDX_DOMAIN_OWNER_DB_ID on domain(owner_db_id);

         create index IDX_TAILORINGREFERENCE_OWNER_DB_ID on tailoringreference(owner_db_id);

         create index IDX_UPDATEREFERENCE_OWNER_DB_ID on updatereference(owner_db_id);

         create index IDX_ELEMENT_TYPE_DEFINITION_OWNER_DB_ID on element_type_definition(owner_db_id);

         create index IDX_CONTROL_RISK_VALUES_ASPECT_OWNER_DB_ID on control_risk_values_aspect(owner_db_id);

         create index IDX_RISKVALUES_ASPECT_OWNER_DB_ID on riskvalues_aspect(owner_db_id);

         create index IDX_SCENARIO_RISK_VALUES_ASPECT_OWNER_DB_ID on scenario_risk_values_aspect(owner_db_id);

         create index IDX_SCOPE_RISK_VALUES_ASPECT_OWNER_DB_ID on scope_risk_values_aspect(owner_db_id);

         create index IDX_PROCESS_IMPACT_VALUES_ASPECT_OWNER_DB_ID on process_impact_values_aspect(owner_db_id);

         create index IDX_DECISION_RESULTS_ASPECT_OWNER_DB_ID on decision_results_aspect(owner_db_id);

         create index IDX_SCOPE_MEMBERS_SCOPE_ID on scope_members(scope_id);

         create index IDX_SCOPE_MEMBERS_MEMBER_ID on scope_members(member_id);

         create index IDX_ELEMENT_DOMAINS_ELEMENT_ID on element_domains(element_db_id);

         create index IDX_ELEMENT_APPLIED_CATALOG_ITEMS_ELEMENT_ID on element_applied_catalog_items(element_db_id);

         create index IDX_ASSET_PARTS_COMPOSITE_ID on asset_parts(composite_id);

         create index IDX_ASSET_PARTS_PART_ID on asset_parts(part_id);

         create index IDX_CONTROL_PARTS_COMPOSITE_ID on control_parts(composite_id);

         create index IDX_CONTROL_PARTS_PART_ID on control_parts(part_id);

         create index IDX_DOCUMENT_PARTS_COMPOSITE_ID on document_parts(composite_id);

         create index IDX_DOCUMENT_PARTS_PART_ID on document_parts(part_id);

         create index IDX_INCIDENT_PARTS_COMPOSITE_ID on incident_parts(composite_id);

         create index IDX_INCIDENT_PARTS_PART_ID on incident_parts(part_id);

         create index IDX_PERSON_PARTS_COMPOSITE_ID on person_parts(composite_id);

         create index IDX_PERSON_PARTS_PART_ID on person_parts(part_id);

         create index IDX_PROCESS_PARTS_COMPOSITE_ID on process_parts(composite_id);

         create index IDX_PROCESS_PARTS_PART_ID on process_parts(part_id);

         create index IDX_SCENARIO_PARTS_COMPOSITE_ID on scenario_parts(composite_id);

         create index IDX_SCENARIO_PARTS_PART_ID on scenario_parts(part_id);

         create index IDX_ELEMENT_DTYPE on element(dtype);

         create index IDX_ELEMENT_OWNER_ID on element(owner_id);

         create index IDX_UNIT_CLIENT_ID on unit(client_id);
""")
    }
}
