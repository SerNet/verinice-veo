/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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

import static java.util.Collections.singleton;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.veo.core.entity.exception.ModelConsistencyException;

/**
 * An entity that is affected by risks resulting from association with
 * scenarios.
 */
public interface RiskAffected<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
        extends Element {

    default void setRisks(Set<R> newRisks) {
        getRisks().clear();
        getRisks().addAll(newRisks);
    }

    Set<R> getRisks();

    default boolean addRisk(R risk) {
        checkRisk(risk);
        return getRisks().add(risk);
    }

    default boolean removeRisk(AbstractRisk<T, R> abstractRisk) {
        return getRisks().remove(abstractRisk);
    }

    default void removeRisks(Set<R> risks) {
        getRisks().removeAll(risks);
    }

    default void checkRisk(R risk) {
        if (!risk.getEntity()
                 .equals(this))
            throw new IllegalArgumentException();
    }

    /**
     * Obtain a risk object for the given scenario in the provided domain. If the
     * risk for this scenario does not yet exist, it will be created, otherwise, the
     * existing risk will be returned. This behavior makes this method idempotent.
     * It can be called with the same parameters multiple times. Subsequent calls
     * after the first one will have no effect.
     *
     * @param scenario
     *            the scenario that causes the risk
     * @param domain
     *            the domain in which this risk is relevant
     * @return the newly created risk object or the existing one if it was
     *         previously created for the scenario in this domain
     */
    R obtainRisk(Scenario scenario, Domain domain);

    default Set<R> obtainRisks(Set<Scenario> scenarios, Domain domain) {
        return getOrCreateRisks(scenarios, singleton(domain));
    }

    default R obtainRisk(Scenario scenario, Set<Domain> domains) {
        if (domains.isEmpty())
            throw new IllegalArgumentException("Need at least one domain to create a risk.");

        var risk = obtainRisk(scenario, domains.stream()
                                               .findFirst()
                                               .orElseThrow());
        domains.forEach(risk::addToDomains);

        return risk;
    }

    default Set<R> getOrCreateRisks(Set<Scenario> scenarios, Set<Domain> domains) {
        scenarios.forEach(s -> s.checkSameClient(this));
        domains.forEach(this::isDomainValid);

        return scenarios.stream()
                        .map(s -> obtainRisk(s, domains))
                        .collect(Collectors.toSet());
    }

    default Optional<R> getRisk(Scenario scenario) {
        return getRisks().stream()
                         .filter(risk -> risk.getScenario()
                                             .equals(scenario))
                         .findFirst();
    }

    default Optional<R> getRisk(Key<UUID> scenarioRef) {
        return getRisks().stream()
                         .filter(risk -> risk.getScenario()
                                             .getId()
                                             .equals(scenarioRef))
                         .findFirst();
    }

    /**
     * Updates an existing risk with new values. Increases the version number of the
     * risk.
     *
     * @param existingRisk
     *            the existing risk value that will be updated with new values
     * @param domains
     *            the new domain list
     * @param mitigation
     *            the new control to mitigate this risk
     * @param riskOwner
     *            the new person to appoint the risk to
     * @return the updated risk entity
     */
    default R updateRisk(R existingRisk, Set<Domain> domains, @Nullable Control mitigation,
            @Nullable Person riskOwner) {

        var riskToUpdate = getRisk(existingRisk.getScenario()).orElseThrow(() -> new IllegalArgumentException(
                String.format("The risk is not know to this object: %s", existingRisk)));

        riskToUpdate.setDomains(domains);
        return riskToUpdate.mitigate(mitigation)
                           .appoint(riskOwner);
    }

    default void isDomainValid(Domain domain) {
        if (!getDomains().contains(domain))
            throw new ModelConsistencyException(
                    "The provided domain '%s' is not yet known to this object. ",
                    domain.getDisplayName());
    }

    @Override
    default boolean removeFromDomains(Domain aDomain) {
        boolean removed = Element.super.removeFromDomains(aDomain);
        if (removed) {
            getRisks().forEach(r -> r.removeFromDomains(aDomain));
        }
        return removed;
    }

    @Override
    default void transferToDomain(Domain oldDomain, Domain newDomain) {
        Element.super.transferToDomain(oldDomain, newDomain);
        getRisks().forEach(r -> r.addToDomains(newDomain));
    }
}
