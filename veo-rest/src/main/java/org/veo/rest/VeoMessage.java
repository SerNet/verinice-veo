/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
package org.veo.rest;

import static org.veo.core.entity.risk.ImpactMethod.Constants.HIGH_WATER_MARK;
import static org.veo.core.entity.risk.ImpactReason.Constants.CUMULATIVE;
import static org.veo.core.entity.risk.ImpactReason.Constants.DISTRIBUTIVE;
import static org.veo.core.entity.risk.ImpactReason.Constants.MANUAL;

/**
 * The keys for the static veo i18n. All entries will be looked up in the application message and
 * sent to the front-end via the {@link org.veo.rest.schemas.controller.TranslationController}.
 */
public enum VeoMessage {
  ABBREVIATION("abbreviation"),
  DESCRIPTION("description"),
  NAME("name"),
  STATUS("status"),
  SUBTYPE("subType"),
  RISK_DEFINITION("riskDefinition"),
  IMPLEMENTATION_STATUS("implementationStatus"),
  POTENTIAL_IMPACT("potentialImpact"),
  POTENTIAL_PROBABILITY("potentialProbability"),
  POTENTIAL_PROBABILITY_EXPLANATION("potentialProbabilityExplanation"),
  ASSET("asset"),
  ASSETS("assets"),
  CONTROL("control"),
  CONTROLS("controls"),
  DOCUMENT("document"),
  DOCUMENTS("documents"),
  INCIDENT("incident"),
  INCIDENTS("incidents"),
  PERSON("person"),
  PERSONS("persons"),
  PROCESS("process"),
  PROCESSES("processes"),
  SCENARIO("scenario"),
  SCENARIOS("scenarios"),
  SCOPE("scope"),
  SCOPES("scopes"),
  IMPACT_REASON_CUMULATIVE(CUMULATIVE),
  IMPACT_REASON_DISTRIBUTION(DISTRIBUTIVE),
  IMPACT_REASON_MANUAL(MANUAL),

  IMPACT_METHOD_HIGH_WATER_MARK(HIGH_WATER_MARK),

  IMPACT_METHOD_HIGH_WATER_MARK_ABBREVIATION(HIGH_WATER_MARK + Constants.ABBREVIATION_SUFFIX),
  IMPACT_REASON_CUMULATIVE_ABBREVIATION(CUMULATIVE + Constants.ABBREVIATION_SUFFIX),
  IMPACT_REASON_DISTRIBUTIVE_ABBREVIATION(DISTRIBUTIVE + Constants.ABBREVIATION_SUFFIX),
  IMPACT_REASON_MANUAL_ABBREVIATION(MANUAL + Constants.ABBREVIATION_SUFFIX);

  private final String messageKey;

  VeoMessage(String messageKey) {
    this.messageKey = messageKey;
  }

  public String getMessageKey() {
    return messageKey;
  }

  private static class Constants {
    public static final String ABBREVIATION_SUFFIX = "_abbreviation";
  }
}
