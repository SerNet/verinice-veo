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
package org.veo.adapter.presenter.api.response.transformer;

import static java.util.Map.copyOf;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.AbstractTailoringReferenceDto;
import org.veo.adapter.presenter.api.dto.AbstractUnitDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.NameableDto;
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.adapter.service.domaintemplate.dto.FullCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.TransformLinkTailoringReference;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.Unit;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.IdRefResolver;

import lombok.RequiredArgsConstructor;

/** A collection of transform functions to transform entities to Dto back and forth. */
@RequiredArgsConstructor
public final class DtoToEntityTransformer {

  private final EntityFactory factory;
  private final IdentifiableFactory identifiableFactory;
  private final EntityStateMapper entityStateMapper;

  public AbstractRisk<?, ?> transformDto2Risk(AbstractRiskDto source, IdRefResolver idRefResolver) {
    if (source instanceof AssetRiskDto ar) {
      return transformDto2AssetRisk(ar, idRefResolver);
    }
    if (source instanceof ProcessRiskDto pr) {
      return transformDto2ProcessRisk(pr, idRefResolver);
    }
    if (source instanceof ScopeRiskDto sr) {
      return transformDto2ScopeRisk(sr, idRefResolver);
    }
    throw new NotImplementedException(
        "Unsupported risk DTO type %s".formatted(source.getClass().getSimpleName()));
  }

  public AssetRisk transformDto2AssetRisk(AssetRiskDto source, IdRefResolver idRefResolver) {
    var asset = idRefResolver.resolve(source.getAsset());
    var risk = mapRisk(source, idRefResolver, asset);
    risk.defineRiskValues(
        source.getDomainsWithRiskValues().values().stream()
            .flatMap(
                domainAssociation ->
                    CategorizedRiskValueMapper.toRiskValues(
                        // domain ID used by DTO may differ from resolved domain's ID
                        idRefResolver.resolve(domainAssociation.getReference()).getIdAsString(),
                        domainAssociation.getRiskDefinitions()))
            .collect(toSet()));
    return risk;
  }

  public ProcessRisk transformDto2ProcessRisk(ProcessRiskDto source, IdRefResolver idRefResolver) {
    var process = idRefResolver.resolve(source.getProcess());
    var risk = mapRisk(source, idRefResolver, process);
    risk.defineRiskValues(
        source.getDomainsWithRiskValues().values().stream()
            .flatMap(
                domainAssociation ->
                    CategorizedRiskValueMapper.toRiskValues(
                        // domain ID used by DTO may differ from resolved domain's ID
                        idRefResolver.resolve(domainAssociation.getReference()).getIdAsString(),
                        domainAssociation.getRiskDefinitions()))
            .collect(toSet()));
    return risk;
  }

  public ScopeRisk transformDto2ScopeRisk(ScopeRiskDto source, IdRefResolver idRefResolver) {
    var scope = idRefResolver.resolve(source.getScope());
    var risk = mapRisk(source, idRefResolver, scope);
    risk.defineRiskValues(
        source.getDomainsWithRiskValues().values().stream()
            .flatMap(
                domainAssociation ->
                    CategorizedRiskValueMapper.toRiskValues(
                        // domain ID used by DTO may differ from resolved domain's ID
                        idRefResolver.resolve(domainAssociation.getReference()).getIdAsString(),
                        domainAssociation.getRiskDefinitions()))
            .collect(toSet()));
    risk.defineRiskValues(
        source.getDomainsWithRiskValues().values().stream()
            .flatMap(
                domainAssociation ->
                    CategorizedRiskValueMapper.toRiskValues(
                        // domain ID used by DTO may differ from resolved domain's ID
                        idRefResolver.resolve(domainAssociation.getReference()).getIdAsString(),
                        domainAssociation.getRiskDefinitions()))
            .collect(toSet()));
    return risk;
  }

  private static <
          TElement extends RiskAffected<TElement, TRisk>,
          TRisk extends AbstractRisk<TElement, TRisk>>
      TRisk mapRisk(AbstractRiskDto source, IdRefResolver idRefResolver, TElement process) {
    var domains =
        source.getDomainReferences().stream().map(idRefResolver::resolve).collect(toSet());
    var risk = process.obtainRisk(idRefResolver.resolve(source.getScenario()), domains);
    risk.mitigate(
        source.getMitigation() != null ? idRefResolver.resolve(source.getMitigation()) : null);
    risk.appoint(
        source.getRiskOwner() != null ? idRefResolver.resolve(source.getRiskOwner()) : null);
    return risk;
  }

  public DomainTemplate transformTransformDomainTemplateDto2DomainTemplate(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(DomainTemplate.class, source);
    mapTransformDomainTemplate(source, idRefResolver, target);
    return target;
  }

  public Domain transformTransformDomainTemplateDto2Domain(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver) {
    // DO NOT use domain template ID as domain ID.
    var target = identifiableFactory.create(Domain.class, Key.newUuid());
    mapTransformDomainTemplate(source, idRefResolver, target);
    target.setActive(true);
    return target;
  }

  // TODO VEO-839 remove when unit import no longer relies on it.
  public Unit transformDto2Unit(AbstractUnitDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(Unit.class, source);
    entityStateMapper.mapState(source, target, idRefResolver);
    return target;
  }

  private void mapTransformDomainTemplate(
      TransformDomainTemplateDto source, IdRefResolver idRefResolver, DomainBase target) {
    target.setAuthority(source.getAuthority());
    target.setTemplateVersion(source.getTemplateVersion());
    mapNameableProperties(source, target);
    target.setElementTypeDefinitions(
        source.getElementTypeDefinitions().entrySet().stream()
            .map(entry -> mapElementTypeDefinition(entry.getKey(), entry.getValue(), target))
            .collect(Collectors.toSet()));
    target.setDecisions(source.getDecisions());
    target.setRiskDefinitions(copyOf(source.getRiskDefinitions()));
    target.setCatalogItems(
        source.getCatalogItems().stream()
            .map(c -> transformDto2CatalogItem(c, idRefResolver))
            .collect(Collectors.toSet()));
    target.setProfiles(copyOf(source.getProfiles()));
  }

  public ElementTypeDefinition mapElementTypeDefinition(
      String type, ElementTypeDefinitionDto source, DomainBase owner) {
    var target = factory.createElementTypeDefinition(type, owner);
    target.setSubTypes(source.getSubTypes());
    target.setCustomAspects(source.getCustomAspects());
    target.setLinks(source.getLinks());
    target.setTranslations(source.getTranslations());
    return target;
  }

  public TailoringReference transformDto2TailoringReference(
      AbstractTailoringReferenceDto source, CatalogItem owner, IdRefResolver idRefResolver) {

    var target =
        source.isLinkTailoringReferences()
            ? createIdentifiable(LinkTailoringReference.class, source)
            : createIdentifiable(TailoringReference.class, source);
    target.setOwner(owner);
    target.setReferenceType(source.getReferenceType());
    if (source.getCatalogItem() != null) {
      CatalogItem resolve = idRefResolver.resolve(source.getCatalogItem());
      target.setTarget(resolve);
    }

    if (source.isLinkTailoringReferences()) {
      TransformLinkTailoringReference tailoringReferenceDto =
          (TransformLinkTailoringReference) source;
      LinkTailoringReference tailoringReference = (LinkTailoringReference) target;
      tailoringReference.setAttributes(tailoringReferenceDto.getAttributes());
      tailoringReference.setLinkType(tailoringReferenceDto.getLinkType());
    }

    return target;
  }

  public <T extends Element> T transformDto2Element(
      AbstractElementDto<T> elementDto, IdRefResolver idRefResolver) {
    T target = (T) createIdentifiable(elementDto.getModelInterface(), elementDto);
    entityStateMapper.mapState(elementDto, target, true, idRefResolver);
    return target;
  }

  private static void mapNameableProperties(NameableDto source, Nameable target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }

  private static <TIn, TOut extends Identifiable> Set<TOut> convertSet(
      Set<TIn> source, Function<TIn, TOut> mapper) {
    if (mapper != null) {
      return source.stream().map(mapper).collect(Collectors.toSet());
    }
    return new HashSet<>();
  }

  public CatalogItem transformDto2CatalogItem(
      FullCatalogItemDto source, IdRefResolver idRefResolver) {
    var target = createIdentifiable(CatalogItem.class, source);
    target.setAbbreviation(source.getAbbreviation());
    target.setName(source.getName());
    target.setDescription(source.getDescription());
    target.setElementType(source.getElementType());
    target.setStatus(source.getStatus());
    target.setSubType(source.getSubType());

    target.setCustomAspects(
        source.getCustomAspects().getValue().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getValue())));

    target.setTailoringReferences(
        convertSet(
            source.getTailoringReferences(),
            tr -> transformDto2TailoringReference(tr, target, idRefResolver)));

    target.setNamespace(source.getNamespace());
    return target;
  }

  private <T extends Identifiable> T createIdentifiable(Class<T> type, Object source) {
    Key<UUID> key = null;
    if (source instanceof IdentifiableDto identifiable) {
      key = Key.uuidFrom(identifiable.getId());
    }
    return identifiableFactory.create(type, key);
  }

  public <T extends Element> T transformDto2Element(
      AbstractElementInDomainDto<T> source, IdRefResolver idRefResolver) {
    T target = (T) createIdentifiable(source.getModelInterface(), source);
    entityStateMapper.mapState(source, target, false, idRefResolver);
    return target;
  }
}
