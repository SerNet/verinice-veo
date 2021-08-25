/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

class V3__rename_custom_properties_to_custom_aspect extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("""

    alter table customproperties rename to custom_aspect;

    alter table customproperties_domains rename to custom_aspect_domains;
    alter table custom_aspect_domains
       rename column customproperties_db_id to custom_aspect_db_id;
    alter table custom_aspect_domains
       rename constraint FK_customproperties_db_id to FK_custom_aspect_db_id;

""")
    }
}
