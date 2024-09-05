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

class V94__profile_product_id extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
                alter table profile add column product_id varchar(120);

                update profile set product_id = name;

                create unique index IDX_PROFILE_DOMAIN_PRODUCT_ID_LANGUAGE
                    on profile(product_id, language, domain_db_id);
                create unique index IDX_PROFILE_DOMAIN_TEMPLATE_PRODUCT_ID_LANGUAGE
                    on profile(product_id, language, domain_template_db_id);

                alter table profile
                    add constraint UK_domain_product_id_language
                        unique using index IDX_PROFILE_DOMAIN_PRODUCT_ID_LANGUAGE deferrable initially deferred,
                    add constraint UK_domain_template_product_id_language
                        unique using index IDX_PROFILE_DOMAIN_TEMPLATE_PRODUCT_ID_LANGUAGE deferrable initially deferred;
        ''')
        }
    }
}
