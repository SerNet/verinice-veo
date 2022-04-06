/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.persistence.entity.jpa;

import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionReferenceFactory;
import org.veo.core.entity.decision.DecisionRuleRef;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @see DecisionReferenceFactory
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DecisionReferenceFactoryImpl extends DecisionReferenceFactory {
    private static DecisionReferenceFactoryImpl instance = new DecisionReferenceFactoryImpl();

    public static DecisionReferenceFactoryImpl getInstance() {
        if (instance == null)
            instance = new DecisionReferenceFactoryImpl();
        return instance;
    }

    @Override
    protected DecisionRef createDecisionRef(String key) {
        return super.createDecisionRef(key);
    }

    @Override
    protected DecisionRuleRef createDecisionRuleRef(Integer intValue) {
        return super.createDecisionRuleRef(intValue);
    }
}
