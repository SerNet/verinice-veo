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

class V63__add_domain_fk_constraints extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            alter table element_domains
                add constraint FK_domain_id
                    foreign key (domains_db_id)
                    references domain;
            alter table subtype_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table control_risk_values_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table decision_results_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table impact_values_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table scenario_risk_values_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table scope_risk_values_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table custom_aspect
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            alter table customlink
                add constraint FK_domain_id
                    foreign key (domain_id)
                    references domain;
            """)
        }
    }
}
