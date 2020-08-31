/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity.code;

import java.time.Instant;
import java.util.function.Predicate;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.util.ModelSwitch;

/**
 * A hand collected set of useful function when working with the entities.
 */
public final class ModelUtils {

    public static final Predicate<Domain> IS_ACTIVE_DOMAIN = Domain::isActive;

    public static void incrementVersion(ModelObject object) {
        object.setValidFrom(Instant.now());
    }

    public static boolean containsDomain(final Domain domain, Object object) {
        ModelSwitch<Boolean> modelSwitch = new ModelSwitch<Boolean>() {
            @Override
            public Boolean caseEntityLayerSupertype(EntityLayerSupertype object) {
                return object.getDomains()
                             .contains(domain);
            }

            @Override
            public Boolean caseClient(Client object) {
                return object.getDomains()
                             .contains(domain);
            }

        };
        return modelSwitch.doOptionalSwitch(object)
                          .orElse(false);
    }

    private ModelUtils() {
    }

}
