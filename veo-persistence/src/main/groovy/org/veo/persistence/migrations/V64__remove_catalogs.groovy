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

class V64__remove_catalogs extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            alter table catalogitem
                add column domain_db_id          varchar(255),
                add column domain_template_db_id varchar(255);

            with s as (select ci.db_id as id, d.db_id as domain_id, dt.db_id as domain_template_id
                       from catalogitem as ci
                                inner join catalog as c on c.db_id = ci.catalog_db_id
                                left join domain as d on d.db_id = c.domaintemplate_id
                                left join domaintemplate as dt on dt.db_id = c.domaintemplate_id)
            update catalogitem
            set domain_db_id = s.domain_id,
                domain_template_db_id = s.domain_template_id
            from s
            where s.id = db_id;

            alter table catalogitem
                drop column catalog_db_id,
                add constraint FK_domain_id
                    foreign key (domain_db_id)
                        references domain,
                add constraint FK_domain_template_id
                    foreign key (domain_template_db_id)
                        references domaintemplate,
                add check ((domain_db_id is null) != (domain_template_db_id is null));

            drop table catalog;
            """)
        }
    }
}
