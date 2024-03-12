/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.service.risk;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.dot.DOTExporter;

import org.veo.core.entity.Asset;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.FlyweightElement;
import org.veo.core.entity.FlyweightLink;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.AssetRepository;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.FlyweightLinkRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ProcessRepository;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.RiskAffectedRepository;
import org.veo.core.repository.ScopeRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ImpactInheritanceCalculatorHighWatermark implements ImpactInheritanceCalculator {

  private final ProcessRepository processRepository;
  private final AssetRepository assetRepository;
  private final ScopeRepository scopeRepository;
  private final FlyweightLinkRepository flyweightRepo;

  @Override
  public Collection<Element> calculateImpactInheritance(
      Unit unit, Domain domain, String riskDefinitionId, RiskAffected<?, ?> affectedElement) {
    long startTime = System.currentTimeMillis();

    log.debug(
        "calculateImpactInheritance '{}' in domain '{}' and rd: '{}'",
        affectedElement.getName(),
        domain.getName(),
        riskDefinitionId);
    Optional<RiskDefinition> ro = domain.getRiskDefinition(riskDefinitionId);
    if (ro.isEmpty()) {
      log.debug(
          "No risk definition for id: {} in domain {}", riskDefinitionId, domain.getDisplayName());
      return Collections.emptyList();
    }
    RiskDefinition riskDefinition = ro.get();
    Set<String> inheritanceLinkTypes = riskLinks(riskDefinition);
    if (inheritanceLinkTypes.isEmpty()) {
      log.debug(
          "no links defined for domain '{}' and risk definition '{}'",
          domain.getDisplayName(),
          riskDefinitionId);
      return Collections.emptyList();
    }
    List<CategoryRef> catRefs =
        riskDefinition.getCategories().stream().map(c -> CategoryRef.from(c)).toList();
    if (catRefs.isEmpty()) {
      log.debug("no Categories defined in {}", riskDefinitionId);
      return Collections.emptyList();
    }

    Set<FlyweightElement> flyweightGraphElements =
        loadFlyweightElements(unit, domain, inheritanceLinkTypes);
    if (flyweightGraphElements.isEmpty()) {
      log.debug("Not a connected Graph");
      return Collections.emptyList();
    }
    // get the affected flyweightElement
    FlyweightElement flyweightElement =
        flyweightGraphElements.stream()
            .filter(t -> t.sourceId().equals(affectedElement.getIdAsString()))
            .findAny()
            .orElse(null);
    if (flyweightElement == null) {
      log.debug("affected element '{}' not in Graph", affectedElement.getName());
      return Collections.emptyList();
    }

    DirectedPseudograph<FlyweightElement, FlyweightLink> completeGraph =
        buildFlyweightGraph(flyweightGraphElements);
    AbstractGraph<Element, CustomLink> affectedGraph =
        buildElementGraph(flyweightElement, completeGraph, domain, unit, inheritanceLinkTypes);

    if (log.isTraceEnabled()) {
      //     export dot graph
      DOTExporter<Element, CustomLink> dotExporter = new DOTExporter<>();
      dotExporter.setVertexIdProvider(t -> "\"" + t.getName() + "\"");
      dotExporter.setEdgeIdProvider(t -> t.getType() + ":");
      StringWriter sw = new StringWriter();
      dotExporter.exportGraph(affectedGraph, sw);
      log.debug("graph:\n{}", sw.toString());
    }

    // test for direct cycles
    CycleDetector<Element, CustomLink> cycleDetector = new CycleDetector<>(affectedGraph);
    RiskDefinitionRef definitionRef = RiskDefinitionRef.from(riskDefinition);
    if (cycleDetector.detectCyclesContainingVertex(affectedElement)) {
      Set<Element> allCycles = cycleDetector.findCycles();
      log.debug(
          "{} is direct part of a cycle: {}", affectedElement.getName(), listNodes(allCycles));
      Set<Element> elementsInCycle = cycleDetector.findCyclesContainingVertex(affectedElement);
      clearCalculatedImpactsInCycle(elementsInCycle, domain, definitionRef);
      return Collections.emptyList();
    }
    Set<Element> elementsPartOfCycle = cycleDetector.findCycles();

    List<Element> changedElements = new ArrayList<>(affectedGraph.vertexSet().size());
    updateAffectedGraph(
        affectedGraph,
        affectedElement,
        domain,
        definitionRef,
        catRefs,
        elementsPartOfCycle,
        changedElements);

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("calculateImpactInheritance took {} ms", timeNeeded);
    return changedElements;
  }

  /**
   * Walks the graph down by following the outgoing links, determine the max impact of the outgoing,
   * compare, update the calculated value and collect all elements affected.
   */
  private void updateAffectedGraph(
      AbstractGraph<Element, CustomLink> elementGraph,
      RiskAffected<?, ?> affectedElement,
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      List<CategoryRef> cats,
      Set<Element> elementsPartOfCycle,
      List<Element> changedElements) {

    Set<CustomLink> outgoingEdges = elementGraph.outgoingEdgesOf(affectedElement);
    Set<CustomLink> incomingEdges = elementGraph.incomingEdgesOf(affectedElement);

    log.debug(
        "updateAffectedGraph: {} incomingEdges: {} outgoingEdges: {}",
        affectedElement.getName(),
        incomingEdges.stream().map(this::toLinkName).collect(Collectors.joining(", ")),
        outgoingEdges.stream().map(this::toLinkName).collect(Collectors.joining(", ")));

    Map<CategoryRef, ImpactRef> maxImpactPerCategorie =
        getMaxImpactPerCategory(domain, riskDefinitionRef, incomingEdges, cats, elementGraph);
    Optional<ImpactValues> impactValues =
        affectedElement.getImpactValues(domain, riskDefinitionRef);
    impactValues.ifPresentOrElse(
        iv -> {
          log.debug(
              "{} set calculated values {}", affectedElement.getName(), maxImpactPerCategorie);
          iv.potentialImpactsCalculated().clear();
          iv.potentialImpactsCalculated().putAll(maxImpactPerCategorie);
          changedElements.add(saveAffectedElement(affectedElement));
        },
        () -> {
          log.debug(
              "{} create calculated values {}", affectedElement.getName(), maxImpactPerCategorie);

          affectedElement.setImpactValues(
              domain,
              Map.of(
                  riskDefinitionRef,
                  new ImpactValues(new HashMap<>(), maxImpactPerCategorie, null, null)));
          changedElements.add(saveAffectedElement(affectedElement));
        });

    if (elementsPartOfCycle.contains(affectedElement)) {
      log.debug(
          "{} is direct part of a cycle: {}",
          affectedElement.getName(),
          listNodes(elementsPartOfCycle));
      clearCalculatedImpactsInCycle(elementsPartOfCycle, domain, riskDefinitionRef);
      // TODO: #2588 we could also return the already affected elements
      return;
    }

    // walk the graph down in a predictable manner
    outgoingEdges.stream()
        .map(e -> elementGraph.getEdgeTarget(e))
        .map(RiskAffected.class::cast)
        .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
        .forEach(
            e ->
                updateAffectedGraph(
                    elementGraph,
                    e,
                    domain,
                    riskDefinitionRef,
                    cats,
                    elementsPartOfCycle,
                    changedElements));
  }

  /**
   * Breaks the complete graph down to the connected set which contains the affectedElement and
   * returns a new sub graph with the concrete elements.
   */
  private AbstractGraph<Element, CustomLink> buildElementGraph(
      FlyweightElement affectedElement,
      DirectedPseudograph<FlyweightElement, FlyweightLink> lightGraph,
      Domain domain,
      Unit unit,
      Collection<String> inheritanceLinkTypes) {
    Graph<FlyweightElement, FlyweightLink> elementSubGraph =
        new BiconnectivityInspector<>(lightGraph).getConnectedComponent(affectedElement);

    Set<RiskAffected<?, ?>> riskAffectedElements =
        loadRiskElements(
            unit,
            domain,
            elementSubGraph.vertexSet().stream()
                .map(v -> v.sourceId())
                .collect(Collectors.toSet()));
    Map<String, RiskAffected<?, ?>> elementById =
        riskAffectedElements.stream()
            .collect(Collectors.toMap(k -> k.getIdAsString(), Function.identity()));

    DirectedPseudograph<Element, CustomLink> graph = new DirectedPseudograph<>(null, null, true);
    riskAffectedElements.forEach(ra -> graph.addVertex(ra));
    riskAffectedElements.stream()
        .flatMap(
            ra ->
                ra.getLinks(domain).stream()
                    .filter(l -> inheritanceLinkTypes.contains(l.getType())))
        .forEach(
            l ->
                graph.addEdge(
                    elementById.get(l.getSource().getIdAsString()),
                    elementById.get(l.getTarget().getIdAsString()),
                    l));

    return graph;
  }

  /** Build the graph for the flyweight elements. */
  private DirectedPseudograph<FlyweightElement, FlyweightLink> buildFlyweightGraph(
      Set<FlyweightElement> flyweightGraphElements) {

    Map<String, FlyweightElement> elementById =
        flyweightGraphElements.stream().collect(Collectors.toMap(t -> t.sourceId(), u -> u));
    DirectedPseudograph<FlyweightElement, FlyweightLink> graph =
        new DirectedPseudograph<>(null, null, true);
    flyweightGraphElements.forEach(ra -> graph.addVertex(ra));
    flyweightGraphElements.stream()
        .flatMap(e -> e.links().stream())
        .forEach(
            l ->
                graph.addEdge(
                    elementById.get(l.getSourceId()), elementById.get(l.getTargetId()), l));

    log.debug(
        "FlyweightGraph- vertexes: {} edges:{}", graph.vertexSet().size(), graph.edgeSet().size());

    return graph;
  }

  /** Returns the max impact values per category of all incoming edges. */
  private Map<CategoryRef, ImpactRef> getMaxImpactPerCategory(
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      Set<CustomLink> incomingEdges,
      List<CategoryRef> catRefs,
      AbstractGraph<Element, CustomLink> maskSubgraph) {

    Map<CategoryRef, List<Entry<CategoryRef, ImpactRef>>> incomingValues =
        incomingEdges.stream()
            .map(l -> maskSubgraph.getEdgeSource(l))
            .map(RiskAffected.class::cast)
            .map(ra -> ra.getImpactValues(domain, riskDefinitionRef))
            .filter(o -> o.isPresent())
            .map(o -> o.get())
            .map(iv -> iv.getPotentialImpactsEffective())
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.groupingBy(e -> e.getKey()));

    Map<CategoryRef, ImpactRef> maxImpacts = new HashMap<>();
    for (CategoryRef categoryRef : catRefs) {
      incomingValues.getOrDefault(categoryRef, Collections.emptyList()).stream()
          .map(a -> a.getValue())
          .collect(Collectors.maxBy((o1, o2) -> o1.getIdRef().compareTo(o2.getIdRef())))
          .ifPresent(i -> maxImpacts.put(categoryRef, ImpactRef.from(i)));
    }
    return maxImpacts;
  }

  private Set<FlyweightElement> loadFlyweightElements(
      Unit unit, Domain domain, Set<String> linkTypes) {
    long startTime = System.currentTimeMillis();
    Set<FlyweightElement> allLinksGroupedByElement =
        flyweightRepo.findAllLinksGroupedByElement(
            linkTypes,
            domain.getIdAsString(),
            unit.getIdAsString(),
            domain.getOwner().getIdAsString());

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("loadFlyweightElements in {} ms", timeNeeded);
    return allLinksGroupedByElement;
  }

  /** Loads all elements with in the given ids Set, uses unit and domain as a filter. */
  private Set<RiskAffected<?, ?>> loadRiskElements(Unit unit, Domain domain, Set<String> ids) {
    long startTime = System.currentTimeMillis();

    HashSet<RiskAffected<?, ?>> allElements = new HashSet<RiskAffected<?, ?>>(ids.size());
    allElements.addAll(queryElements(unit, domain, processRepository, ids));
    allElements.addAll(queryElements(unit, domain, assetRepository, ids));
    allElements.addAll(queryElements(unit, domain, scopeRepository, ids));

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("loadRiskElements: {} elements loaded in {} ms", allElements.size(), timeNeeded);
    return allElements;
  }

  /** Queries the elements by ids. */
  private List<? extends RiskAffected<?, ?>> queryElements(
      Unit unit,
      Domain domain,
      RiskAffectedRepository<? extends RiskAffected<?, ?>, ?> repository,
      Set<String> ids) {
    ElementQuery<? extends RiskAffected<?, ?>> query = repository.query(unit.getClient());
    query.whereOwnerIs(unit);
    query.whereDomainsContain(domain);
    query.whereIdIn(new QueryCondition<String>(ids));

    return query.execute(PagingConfiguration.UNPAGED).getResultPage().stream().toList();
  }

  private RiskAffected<?, ?> saveAffectedElement(RiskAffected<?, ?> affectedElement) {
    if (affectedElement instanceof Asset asset) {
      return assetRepository.save(asset);
    } else if (affectedElement instanceof Process process) {
      return processRepository.save(process);
    } else if (affectedElement instanceof Scope scope) {
      return scopeRepository.save(scope);
    }
    throw new IllegalArgumentException();
  }

  private void clearCalculatedImpactsInCycle(
      Set<Element> elementsInCycle, Domain domain, RiskDefinitionRef riskDefinitionRef) {
    log.debug("clear calculated impact in elements : {}", listNodes(elementsInCycle));

    elementsInCycle.stream()
        .map(RiskAffected.class::cast)
        .forEach(
            e ->
                e.getImpactValues(domain, riskDefinitionRef)
                    .ifPresent(
                        iv -> {
                          iv.potentialImpactsCalculated().clear();
                          saveAffectedElement(e);
                        }));
  }

  private Set<String> riskLinks(RiskDefinition riskDefinition) {
    return riskDefinition.getImpactInheritingLinks().values().stream()
        .flatMap(e -> e.stream())
        .collect(Collectors.toSet());
  }

  private String listNodes(Set<Element> allCycles) {
    return allCycles.stream().map(e -> e.getName()).collect(Collectors.joining(", "));
  }

  private String toLinkName(CustomLink l) {
    return l.getSource().getName() + "->" + l.getTarget().getName();
  }
}
