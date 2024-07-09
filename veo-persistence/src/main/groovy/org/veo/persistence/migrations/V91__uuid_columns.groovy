/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

class V91__uuid_columns extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
                ALTER TABLE abstractriskdata_domains DROP CONSTRAINT FK_abstractriskdata_db_id;
                ALTER TABLE abstractriskdata_domains DROP CONSTRAINT FK_domains_db_id;
                ALTER TABLE abstractriskdata DROP CONSTRAINT FK_control_id;
                ALTER TABLE abstractriskdata DROP CONSTRAINT FK_entity_db_id;
                ALTER TABLE abstractriskdata DROP CONSTRAINT FK_person_id;
                ALTER TABLE abstractriskdata DROP CONSTRAINT FK_scenario_db_id;
                ALTER TABLE asset_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE asset_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE catalogitem DROP CONSTRAINT FK_domain_id;
                ALTER TABLE catalogitem DROP CONSTRAINT FK_domain_template_id;
                ALTER TABLE catalog_tailoring_reference DROP CONSTRAINT FK_catalog_item_db_id;
                ALTER TABLE catalog_tailoring_reference DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE control_implementation DROP CONSTRAINT FK_control;
                ALTER TABLE control_implementation DROP CONSTRAINT FK_owner;
                ALTER TABLE control_implementation DROP CONSTRAINT FK_responsible;
                ALTER TABLE control_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE control_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE control_risk_values_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE control_risk_values_aspect DROP CONSTRAINT FK_owner_id;
                ALTER TABLE custom_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE custom_aspect DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE customlink DROP CONSTRAINT FK_domain_id;
                ALTER TABLE decision_results_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE decision_results_aspect DROP CONSTRAINT FK_owner_id;
                ALTER TABLE document_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE document_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE domain DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE element_applied_catalog_items DROP CONSTRAINT FK_applied_catalog_items_db_id;
                ALTER TABLE element_applied_catalog_items DROP CONSTRAINT FK_element_db_id;
                ALTER TABLE element_domains DROP CONSTRAINT FK_domain_id;
                ALTER TABLE element_domains DROP CONSTRAINT FK_domains_db_id;
                ALTER TABLE element_domains DROP CONSTRAINT FK_element_db_id;
                ALTER TABLE element DROP CONSTRAINT FK_owner_id;
                ALTER TABLE impact_values_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE impact_values_aspect DROP CONSTRAINT FK_owner_id;
                ALTER TABLE incident_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE incident_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE person_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE person_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE process_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE process_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE profile DROP CONSTRAINT FK_domain_id;
                ALTER TABLE profile DROP CONSTRAINT FK_domain_template_id;
                ALTER TABLE profile_item DROP CONSTRAINT FK_applied_catalog_item_id;
                ALTER TABLE profile_item DROP CONSTRAINT FK_owner_id;
                ALTER TABLE profile_tailoring_reference DROP CONSTRAINT FK_catalog_item_id;
                ALTER TABLE profile_tailoring_reference DROP CONSTRAINT FK_mitigation_id;
                ALTER TABLE profile_tailoring_reference DROP CONSTRAINT FK_owner_id;
                ALTER TABLE profile_tailoring_reference DROP CONSTRAINT FK_responsible_id;
                ALTER TABLE profile_tailoring_reference DROP CONSTRAINT FK_risk_owner_id;
                ALTER TABLE requirement_implementation DROP CONSTRAINT FK_control;
                ALTER TABLE requirement_implementation DROP CONSTRAINT FK_origin;
                ALTER TABLE requirement_implementation DROP CONSTRAINT FK_responsible;
                ALTER TABLE riskvalues_aspect DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE scenario_parts DROP CONSTRAINT FK_composite_id;
                ALTER TABLE scenario_parts DROP CONSTRAINT FK_part_id;
                ALTER TABLE scenario_risk_values_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE scenario_risk_values_aspect DROP CONSTRAINT FK_owner_id;
                ALTER TABLE scope_members DROP CONSTRAINT FK_member_id;
                ALTER TABLE scope_members DROP CONSTRAINT FK_scope_id;
                ALTER TABLE scope_risk_values_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE scope_risk_values_aspect DROP CONSTRAINT FK_owner_id;
                ALTER TABLE subtype_aspect DROP CONSTRAINT FK_domain_id;
                ALTER TABLE subtype_aspect DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE unit_domains DROP CONSTRAINT FK_domains_db_id;
                ALTER TABLE unit_domains DROP CONSTRAINT FK_unit_db_id;
                ALTER TABLE unit DROP CONSTRAINT FK_client_id;
                ALTER TABLE unit DROP CONSTRAINT FK_parent_db_id;
                ALTER TABLE updatereference DROP CONSTRAINT FK_catalog_item_db_id;
                ALTER TABLE updatereference DROP CONSTRAINT FK_owner_db_id;
                ALTER TABLE userconfiguration DROP CONSTRAINT FK_client_id;

                ALTER TABLE abstractriskdata ALTER COLUMN control_id TYPE uuid USING control_id::uuid;
                ALTER TABLE abstractriskdata ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE abstractriskdata ALTER COLUMN entity_db_id TYPE uuid USING entity_db_id::uuid;
                ALTER TABLE abstractriskdata ALTER COLUMN person_id TYPE uuid USING person_id::uuid;
                ALTER TABLE abstractriskdata ALTER COLUMN scenario_db_id TYPE uuid USING scenario_db_id::uuid;
                ALTER TABLE abstractriskdata_domains ALTER COLUMN abstractriskdata_db_id TYPE uuid USING abstractriskdata_db_id::uuid;
                ALTER TABLE abstractriskdata_domains ALTER COLUMN domains_db_id TYPE uuid USING domains_db_id::uuid;
                ALTER TABLE asset_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE asset_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE catalogitem ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE catalogitem ALTER COLUMN domain_db_id TYPE uuid USING domain_db_id::uuid;
                ALTER TABLE catalogitem ALTER COLUMN domain_template_db_id TYPE uuid USING domain_template_db_id::uuid;
                ALTER TABLE catalog_tailoring_reference ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE catalog_tailoring_reference ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE catalog_tailoring_reference ALTER COLUMN target_db_id TYPE uuid USING target_db_id::uuid;
                ALTER TABLE client ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE control_implementation ALTER COLUMN control_id TYPE uuid USING control_id::uuid;
                ALTER TABLE control_implementation ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE control_implementation ALTER COLUMN person_id TYPE uuid USING person_id::uuid;
                ALTER TABLE control_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE control_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE control_risk_values_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE control_risk_values_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE control_risk_values_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE custom_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE custom_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE custom_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE customlink ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE customlink ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE customlink ALTER COLUMN source_id TYPE uuid USING source_id::uuid;
                ALTER TABLE customlink ALTER COLUMN target_id TYPE uuid USING target_id::uuid;
                ALTER TABLE decision_results_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE decision_results_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE decision_results_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE document_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE document_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE domain ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE domain ALTER COLUMN domain_template_db_id TYPE uuid USING domain_template_db_id::uuid;
                ALTER TABLE domain ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE domaintemplate ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE element ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE element ALTER COLUMN owner_id TYPE uuid USING owner_id::uuid;
                ALTER TABLE element_applied_catalog_items ALTER COLUMN applied_catalog_items_db_id TYPE uuid USING applied_catalog_items_db_id::uuid;
                ALTER TABLE element_applied_catalog_items ALTER COLUMN element_db_id TYPE uuid USING element_db_id::uuid;
                ALTER TABLE element_domains ALTER COLUMN domains_db_id TYPE uuid USING domains_db_id::uuid;
                ALTER TABLE element_domains ALTER COLUMN element_db_id TYPE uuid USING element_db_id::uuid;
                ALTER TABLE element_type_definition ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE element_type_definition ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE impact_values_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE impact_values_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE impact_values_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE incident_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE incident_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE person_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE person_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE process_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE process_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE profile ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE profile ALTER COLUMN domain_db_id TYPE uuid USING domain_db_id::uuid;
                ALTER TABLE profile ALTER COLUMN domain_template_db_id TYPE uuid USING domain_template_db_id::uuid;
                ALTER TABLE profile_item ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE profile_item ALTER COLUMN applied_catalog_item_db_id TYPE uuid USING applied_catalog_item_db_id::uuid;
                ALTER TABLE profile_item ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN mitigation_db_id TYPE uuid USING mitigation_db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN responsible_db_id TYPE uuid USING responsible_db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN risk_owner_db_id TYPE uuid USING risk_owner_db_id::uuid;
                ALTER TABLE profile_tailoring_reference ALTER COLUMN target_db_id TYPE uuid USING target_db_id::uuid;
                ALTER TABLE requirement_implementation ALTER COLUMN control_id TYPE uuid USING control_id::uuid;
                ALTER TABLE requirement_implementation ALTER COLUMN origin_db_id TYPE uuid USING origin_db_id::uuid;
                ALTER TABLE requirement_implementation ALTER COLUMN person_id TYPE uuid USING person_id::uuid;
                ALTER TABLE riskvalues_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE riskvalues_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE riskvalues_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE scenario_parts ALTER COLUMN composite_id TYPE uuid USING composite_id::uuid;
                ALTER TABLE scenario_parts ALTER COLUMN part_id TYPE uuid USING part_id::uuid;
                ALTER TABLE scenario_risk_values_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE scenario_risk_values_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE scenario_risk_values_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE scope_members ALTER COLUMN member_id TYPE uuid USING member_id::uuid;
                ALTER TABLE scope_members ALTER COLUMN scope_id TYPE uuid USING scope_id::uuid;
                ALTER TABLE scope_risk_values_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE scope_risk_values_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE scope_risk_values_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE subtype_aspect ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE subtype_aspect ALTER COLUMN domain_id TYPE uuid USING domain_id::uuid;
                ALTER TABLE subtype_aspect ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE unit ALTER COLUMN client_id TYPE uuid USING client_id::uuid;
                ALTER TABLE unit ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE unit ALTER COLUMN parent_db_id TYPE uuid USING parent_db_id::uuid;
                ALTER TABLE unit_domains ALTER COLUMN domains_db_id TYPE uuid USING domains_db_id::uuid;
                ALTER TABLE unit_domains ALTER COLUMN unit_db_id TYPE uuid USING unit_db_id::uuid;
                ALTER TABLE updatereference ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE updatereference ALTER COLUMN owner_db_id TYPE uuid USING owner_db_id::uuid;
                ALTER TABLE updatereference ALTER COLUMN target_db_id TYPE uuid USING target_db_id::uuid;
                ALTER TABLE userconfiguration ALTER COLUMN db_id TYPE uuid USING db_id::uuid;
                ALTER TABLE userconfiguration ALTER COLUMN client_id TYPE uuid USING client_id::uuid;

                ALTER TABLE abstractriskdata ADD CONSTRAINT FK_control_id FOREIGN KEY (control_id) REFERENCES element;
                ALTER TABLE abstractriskdata ADD CONSTRAINT FK_entity_db_id FOREIGN KEY (entity_db_id) REFERENCES element;
                ALTER TABLE abstractriskdata ADD CONSTRAINT FK_person_id FOREIGN KEY (person_id) REFERENCES element;
                ALTER TABLE abstractriskdata ADD CONSTRAINT FK_scenario_db_id FOREIGN KEY (scenario_db_id) REFERENCES element;
                ALTER TABLE abstractriskdata_domains ADD CONSTRAINT FK_abstractriskdata_db_id FOREIGN KEY (abstractriskdata_db_id) REFERENCES abstractriskdata(db_id);
                ALTER TABLE abstractriskdata_domains ADD CONSTRAINT FK_domains_db_id FOREIGN KEY (domains_db_id) REFERENCES domain(db_id);
                ALTER TABLE asset_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE asset_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE catalogitem ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_db_id) REFERENCES domain(db_id);
                ALTER TABLE catalogitem ADD CONSTRAINT FK_domain_template_id FOREIGN KEY (domain_template_db_id) REFERENCES domaintemplate(db_id);
                ALTER TABLE catalog_tailoring_reference ADD CONSTRAINT FK_target_db_id FOREIGN KEY (target_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE catalog_tailoring_reference ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE control_implementation ADD CONSTRAINT FK_control FOREIGN KEY (control_id) REFERENCES element(db_id);
                ALTER TABLE control_implementation ADD CONSTRAINT FK_owner FOREIGN KEY (owner_db_id) REFERENCES element(db_id);
                ALTER TABLE control_implementation ADD CONSTRAINT FK_responsible FOREIGN KEY (person_id) REFERENCES element(db_id);
                ALTER TABLE control_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE control_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE control_risk_values_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE control_risk_values_aspect ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE custom_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE custom_aspect ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE customlink ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE decision_results_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE decision_results_aspect ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE document_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE document_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE domain ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES client(db_id);
                ALTER TABLE element ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_id) REFERENCES unit(db_id);
                ALTER TABLE element_applied_catalog_items ADD CONSTRAINT FK_applied_catalog_items_db_id FOREIGN KEY (applied_catalog_items_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE element_applied_catalog_items ADD CONSTRAINT FK_element_db_id FOREIGN KEY (element_db_id) REFERENCES element(db_id);
                ALTER TABLE element_domains ADD CONSTRAINT FK_domain_id FOREIGN KEY (domains_db_id) REFERENCES domain(db_id);
                ALTER TABLE element_domains ADD CONSTRAINT FK_element_db_id FOREIGN KEY (element_db_id) REFERENCES element(db_id);
                ALTER TABLE impact_values_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE impact_values_aspect ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE incident_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE incident_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE person_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE person_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE process_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE process_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE profile ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_db_id) REFERENCES domain(db_id);
                ALTER TABLE profile ADD CONSTRAINT FK_domain_template_id FOREIGN KEY (domain_template_db_id) REFERENCES domaintemplate(db_id);
                ALTER TABLE profile_item ADD CONSTRAINT FK_applied_catalog_item_id FOREIGN KEY (applied_catalog_item_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE profile_item ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES profile(db_id);
                ALTER TABLE profile_tailoring_reference ADD CONSTRAINT FK_target_id FOREIGN KEY (target_db_id) REFERENCES profile_item(db_id);
                ALTER TABLE profile_tailoring_reference ADD CONSTRAINT FK_mitigation_id FOREIGN KEY (mitigation_db_id) REFERENCES profile_item(db_id);
                ALTER TABLE profile_tailoring_reference ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES profile_item(db_id);
                ALTER TABLE profile_tailoring_reference ADD CONSTRAINT FK_responsible_id FOREIGN KEY (responsible_db_id) REFERENCES profile_item(db_id);
                ALTER TABLE profile_tailoring_reference ADD CONSTRAINT FK_risk_owner_id FOREIGN KEY (risk_owner_db_id) REFERENCES profile_item(db_id);
                ALTER TABLE requirement_implementation ADD CONSTRAINT FK_control FOREIGN KEY (control_id) REFERENCES element(db_id);
                ALTER TABLE requirement_implementation ADD CONSTRAINT FK_origin FOREIGN KEY (origin_db_id) REFERENCES element(db_id);
                ALTER TABLE requirement_implementation ADD CONSTRAINT FK_responsible FOREIGN KEY (person_id) REFERENCES element(db_id);
                ALTER TABLE riskvalues_aspect ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES abstractriskdata;
                ALTER TABLE scenario_parts ADD CONSTRAINT FK_composite_id FOREIGN KEY (composite_id) REFERENCES element(db_id);
                ALTER TABLE scenario_parts ADD CONSTRAINT FK_part_id FOREIGN KEY (part_id) REFERENCES element(db_id);
                ALTER TABLE scenario_risk_values_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE scenario_risk_values_aspect ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE scope_members ADD CONSTRAINT FK_member_id FOREIGN KEY (member_id) REFERENCES element(db_id);
                ALTER TABLE scope_members ADD CONSTRAINT FK_scope_id FOREIGN KEY (scope_id) REFERENCES element(db_id);
                ALTER TABLE scope_risk_values_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE scope_risk_values_aspect ADD CONSTRAINT FK_owner_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE subtype_aspect ADD CONSTRAINT FK_domain_id FOREIGN KEY (domain_id) REFERENCES domain(db_id);
                ALTER TABLE subtype_aspect ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES element;
                ALTER TABLE unit ADD CONSTRAINT FK_client_id FOREIGN KEY (client_id) REFERENCES client(db_id);
                ALTER TABLE unit ADD CONSTRAINT FK_parent_db_id FOREIGN KEY (parent_db_id) REFERENCES unit(db_id);
                ALTER TABLE unit_domains ADD CONSTRAINT FK_domains_db_id FOREIGN KEY (domains_db_id) REFERENCES domain(db_id);
                ALTER TABLE unit_domains ADD CONSTRAINT FK_unit_db_id FOREIGN KEY (unit_db_id) REFERENCES unit(db_id);
                ALTER TABLE updatereference ADD CONSTRAINT FK_catalog_item_db_id FOREIGN KEY (target_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE updatereference ADD CONSTRAINT FK_owner_db_id FOREIGN KEY (owner_db_id) REFERENCES catalogitem(db_id);
                ALTER TABLE userconfiguration ADD CONSTRAINT FK_client_id FOREIGN KEY (client_id) REFERENCES client(db_id);
        ''')
        }
    }
}
