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
package org.veo.persistence.entity.jpa;

import java.math.BigDecimal;

import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ImplementationStatusRef;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskRef;
import org.veo.core.entity.risk.RiskReferenceProvider;

/**
 * Simple factory for reference objects to be used by JPA converters. Should not
 * be used outside the package by converters - use a {@code ReferenceProvider}
 * instead which ensures that references cannot be created outside their valid
 * scope.
 *
 * @see RiskReferenceProvider
 */
final class RiskReferenceFactoryImpl extends org.veo.core.entity.risk.RiskReferenceFactory {

    private static RiskReferenceFactoryImpl instance = new RiskReferenceFactoryImpl();

    private RiskReferenceFactoryImpl() {
    }

    public static RiskReferenceFactoryImpl getInstance() {
        if (instance == null)
            instance = new RiskReferenceFactoryImpl();
        return instance;
    }

    @Override
    protected RiskRef createRiskRef(BigDecimal id) {
        return super.createRiskRef(id);
    }

    @Override
    protected ProbabilityRef createProbabilityRef(BigDecimal id) {
        return super.createProbabilityRef(id);
    }

    @Override
    protected ImpactRef createImpactRef(BigDecimal id) {
        return super.createImpactRef(id);
    }

    @Override
    protected CategoryRef createCategoryRef(String id) {
        return super.createCategoryRef(id);
    }

    @Override
    protected RiskDefinitionRef createRiskDefinitionRef(String dbData) {
        return super.createRiskDefinitionRef(dbData);
    }

    @Override
    protected ImplementationStatusRef createImplementationStatusRef(int ordinalValue) {
        return super.createImplementationStatusRef(ordinalValue);
    }
}
