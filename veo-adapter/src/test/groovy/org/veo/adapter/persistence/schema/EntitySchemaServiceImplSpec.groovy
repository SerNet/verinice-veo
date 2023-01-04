/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.adapter.persistence.schema

import spock.lang.Specification

class EntitySchemaServiceImplSpec extends Specification {

    def "Comparing locale #entry against the requested locales #req returns: #match"() {
        expect:
        var reqLocales = req.collect { l -> Locale.forLanguageTag(l) } as Set
        var entryLocale = Locale.forLanguageTag(entry)
        EntitySchemaServiceImpl.isRequested(reqLocales, entryLocale) == match

        where:
        req                | entry   | match
        ["de", "en"]       | "de"    | true
        ["de"]             | "de"    | true
        ["de", "en"]       | "en"    | true
        ["de-CH", "en"]    | "de"    | true // return language if region does not match
        ["de-CH", "en"]    | "de-CH" | true
        ["de-CH", "en"]    | "de-CH" | true
        ["de-CH", "en-US"] | "en"    | true
        ["de-CH", "en-US"] | "en-US" | true
        ["de", "en"]       | "tlh"   | false
        ["de"]             | "tlh"   | false
        ["tlh"]            | "de"    | false
        ["de-CH", "en"]    | "de-DE" | false // do not return region B for requested region A
        ["en-US"]          | "de-CH" | false
        ["de", "en"]       | "de-CH" | false
        ["vulcan"]         | "en"    | false
        ["vulcan"]         | "de"    | false // notable exception: risk definitions, see VEO-1739
    }
}
