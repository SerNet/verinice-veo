/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.core.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base for all the custom aspects. This class should be extensible by
 * clients.
 */
public interface CustomProperties {

    int MAXIMUM_STRING_LENGTH = 18000;

    String getType();

    void setType(String aType);

    Set<String> getApplicableTo();

    void setApplicableTo(Set<String> aApplicableTo);

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    boolean addToDomains(Domain aDomain);

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    boolean removeFromDomains(Domain aDomain);

    Set<Domain> getDomains();

    void setDomains(Set<Domain> aDomains);

    void clearProperties();

    Map<String, Boolean> getBooleanProperties();

    void setProperty(String key, Boolean value);

    Map<String, Double> getDoubleProperties();

    void setProperty(String key, Double value);

    Map<String, Integer> getIntegerProperties();

    void setProperty(String key, Integer value);

    Map<String, OffsetDateTime> getOffsetDateTimeProperties();

    void setProperty(String key, OffsetDateTime value);

    Map<String, String> getStringProperties();

    void setProperty(String key, String value);

    Map<String, List<String>> getStringListProperties();

    void setProperty(String key, List<String> value);

    // @formatter:off
    /**
     * W.l.o.g. we assume this CustomAspect belongs to a person.
     *
     * The (jackson) serialized map then has to conform to
     * Person.json#properties.customAspects.properties.{this.getType()}.properties.attributes.properties.
     *
     * Person.json#properties.customAspects.properties.{this.getType()}
     * has to conform to custom-aspect-meta-schema.json.
     */
    // @formatter:on
    Map<String, ?> getAllProperties();

}
