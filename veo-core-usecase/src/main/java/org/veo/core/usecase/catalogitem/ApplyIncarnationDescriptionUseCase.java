/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.usecase.catalogitem;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Catalogable;
import org.veo.core.entity.Client;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.EntityLayerSupertypeRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.CatalogItemService;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseTools;
import org.veo.core.usecase.parameter.IncarnateCatalogItemDescription;
import org.veo.core.usecase.parameter.TailoringReferenceParameter;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ApplyIncarnationDescriptionUseCase implements
        TransactionalUseCase<ApplyIncarnationDescriptionUseCase.InputData, ApplyIncarnationDescriptionUseCase.OutputData> {
    private final UnitRepository unitRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final org.veo.core.repository.RepositoryProvider repositoryProvider;
    private final DesignatorService designatorService;
    private final CatalogItemService catalogItemservice;

    @Override
    public OutputData execute(InputData input) {
        log.info("ApplyIncarnationDescriptionUseCase: {}", input);
        Unit unit = unitRepository.findById(input.getContainerId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getContainerId()));
        Client authenticatedClient = input.authenticatedClient;
        unit.checkSameClient(authenticatedClient);

        List<Catalogable> createdCatalogables = input.getReferecesToApply()
                                                     .stream()
                                                     .map(ra -> {
                                                         CatalogItem catalogItem = catalogItemRepository.findById(ra.getItem()
                                                                                                                    .getId())
                                                                                                        .orElseThrow(() -> new NotFoundException(
                                                                                                                "CatalogItem not found %s",
                                                                                                                ra.getItem()
                                                                                                                  .getId()));

                                                         Domain domain = (Domain) catalogItem.getCatalog()
                                                                                             .getDomainTemplate();
                                                         UseCaseTools.checkDomainBelongsToClient(input.getAuthenticatedClient(),
                                                                                                 domain);
                                                         return createElementFromItem(unit,
                                                                                      authenticatedClient,
                                                                                      catalogItem,
                                                                                      domain,
                                                                                      ra.getReferences());
                                                     })
                                                     .collect(Collectors.toList());
        log.info("ApplyIncarnationDescriptionUseCase elements created: {}", createdCatalogables);
        return new ApplyIncarnationDescriptionUseCase.OutputData(createdCatalogables);
    }

    private EntityLayerSupertype createElementFromItem(Unit unit, Client authenticatedClient,
            CatalogItem catalogItem, Domain domain,
            List<TailoringReferenceParameter> referecesToApply) {
        validateItem(catalogItem, referecesToApply);
        Catalogable copyItem = catalogItemservice.createInstance(catalogItem, domain);
        applyTailoringReferences(copyItem, referecesToApply);
        @SuppressWarnings("unchecked")
        EntityLayerSupertypeRepository<EntityLayerSupertype> repository = repositoryProvider.getEntityLayerSupertypeRepositoryFor((Class<EntityLayerSupertype>) copyItem.getModelInterface());
        EntityLayerSupertype entity = (EntityLayerSupertype) copyItem;
        entity.setOwner(unit);
        designatorService.assignDesignator(entity, authenticatedClient);
        entity = repository.save(entity);
        return entity;
    }

    private void validateItem(CatalogItem catalogItem,
            List<TailoringReferenceParameter> referecesToApply) {
        if (catalogItem.getTailoringReferences()
                       .stream()
                       .filter(UseCaseTools.IS_LINK_PREDICATE)
                       .count() != referecesToApply.size()) {
            throw new IllegalArgumentException("Tailoring references don't match.");
        }
    }

    private void applyTailoringReferences(Catalogable copyItem,
            List<TailoringReferenceParameter> referencesToApply) {
        if (copyItem instanceof EntityLayerSupertype) {
            EntityLayerSupertype el = (EntityLayerSupertype) copyItem;
            List<CustomLink> orderByExecution = el.getLinks()
                                                  .stream()
                                                  .sorted(UseCaseTools.BY_LINK_EXECUTION)
                                                  .collect(Collectors.toList());

            if (orderByExecution.size() > referencesToApply.size()) {
                throw new IllegalArgumentException(
                        "Number of defined links cannot be smaller than number of references to apply.");
            }

            for (int i = 0; i < orderByExecution.size(); i++) {
                CustomLink customLink = orderByExecution.get(i);
                customLink.setTarget((EntityLayerSupertype) referencesToApply.get(i)
                                                                             .getReferencedCatalogable());
            }
            // TODO: VEO-612 handle parts
        }
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Client authenticatedClient;
        Key<UUID> containerId;
        List<IncarnateCatalogItemDescription> referecesToApply;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        List<Catalogable> newElements;
    }

}
