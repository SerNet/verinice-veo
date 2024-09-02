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

import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Nameable;
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
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ImpactInheritanceCalculatorHighWatermark implements ImpactInheritanceCalculator {

  @NoArgsConstructor
  class FlyweightImpactInheritenceContext {
    Unit unit;
    Domain domain;
    RiskDefinition riskDefinition;
    RiskDefinitionRef definitionRef;
    Set<String> inheritanceLinkTypes;
    List<CategoryRef> catRefs;
    Set<FlyweightElement> flyweightGraphElements;
    Graph<FlyweightElement, FlyweightLink> completeGraph;

    boolean hasValidGraph() {
      return isInheritanceActive() && flyweightGraphElements != null && completeGraph != null;
    }

    boolean isInheritanceActive() {
      return riskDefinition != null
          && definitionRef != null
          && inheritanceLinkTypes != null
          && !inheritanceLinkTypes.isEmpty()
          && catRefs != null
          && !catRefs.isEmpty();
    }
  }

  final class UpdateAffectedGraphParameter {
    AbstractGraph<Element, CustomLink> elementGraph;
    Domain domain;
    RiskDefinitionRef riskDefinitionRef;
    List<CategoryRef> cats;
    Graph<FlyweightElement, FlyweightLink> elementSubGraph;
    CycleDetector<Element, CustomLink> cycleDetector;
    Set<RiskAffected<?, ?>> riskAffectedCache;

    private UpdateAffectedGraphParameter(
        AbstractGraph<Element, CustomLink> elementGraph,
        Domain domain,
        List<CategoryRef> cats,
        RiskDefinitionRef definitionRef,
        Graph<FlyweightElement, FlyweightLink> elementSubGraph,
        Set<RiskAffected<?, ?>> riskAffectedCache) {
      this.elementGraph = elementGraph;
      this.domain = domain;
      this.cats = cats;
      this.riskDefinitionRef = definitionRef;
      this.elementSubGraph = elementSubGraph;
      this.riskAffectedCache = riskAffectedCache;
      cycleDetector = new CycleDetector<>(elementGraph);
    }

    Set<Element> findCyclesContainingVertex(Element v) {
      return cycleDetector.findCyclesContainingVertex(v);
    }

    boolean detectCyclesContainingVertex(Element v) {
      return cycleDetector.detectCyclesContainingVertex(v);
    }
  }

  private final ProcessRepository processRepository;
  private final AssetRepository assetRepository;
  private final ScopeRepository scopeRepository;
  private final FlyweightLinkRepository flyweightRepo;

  @Override
  public Collection<? extends Element> updateAllRootNodes(
      Unit unit, Domain domain, String riskDefinitionId) {
    long startTime = System.currentTimeMillis();

    log.debug(
        "updateAllRootNodes for '{}' in domain '{}' and rd: '{}'",
        unit.getName(),
        domain.getName(),
        riskDefinitionId);
    FlyweightImpactInheritenceContext data = prepareData(unit, domain, riskDefinitionId);
    if (!data.isInheritanceActive()) {
      log.debug(
          "Inheritance not active in unit: {} with domain {} for riskdefinition: {}/{} with linktypes: {}",
          data.unit.getName(),
          data.domain.getName(),
          data.riskDefinition,
          data.catRefs,
          data.inheritanceLinkTypes);
      return Collections.emptyList();
    }
    if (!data.hasValidGraph()) {
      log.debug(
          "No valid graph in unit: {} with domain {} for riskdefinition: {}, flyweight elements: {}",
          data.unit.getName(),
          data.domain.getName(),
          data.riskDefinition,
          data.flyweightGraphElements);
      // there might be one, or more elements without any connection
      // we would load all riskaffected and clear the calculated values
      // TODO: #2908 examine and handle edge cases, like unconnected nodes
      return Collections.emptyList();
    }
    Set<RiskAffected<?, ?>> allRiskElements = loadAllRiskElements(unit, domain);

    // we group the elements by the number of incoming edges
    Map<Integer, List<FlyweightElement>> inDegree =
        data.completeGraph.vertexSet().stream()
            .collect(Collectors.groupingBy(v -> data.completeGraph.inDegreeOf(v)));
    // we select the root elements, which are all elements with zero incoming edges
    List<FlyweightElement> listOfRootElements = inDegree.get(0);
    if (listOfRootElements == null) {
      Set<FlyweightElement> cycles = new CycleDetector<>(data.completeGraph).findCycles();
      Set<RiskAffected<?, ?>> elementsInCycle =
          mapIdsToRiskAffected(allRiskElements, toIds(cycles));
      log.atInfo()
          .setMessage("no roots, may be circles: {}")
          .addArgument(() -> listNodes(elementsInCycle))
          .log();
      clearCalculatedImpactsInCycle(elementsInCycle, domain, data.definitionRef);
      return elementsInCycle;
    }

    Set<RiskAffected<?, ?>> allRootelements =
        mapIdsToRiskAffected(allRiskElements, toIds(listOfRootElements));
    Map<String, RiskAffected<?, ?>> idToElement =
        allRootelements.stream().collect(toMap(Identifiable::getIdAsString, identity()));
    Set<FlyweightElement> processed = new HashSet<>();
    ArrayList<Element> changedElements = new ArrayList<>(data.completeGraph.vertexSet().size());

    listOfRootElements.stream()
        .filter(notProcessed(processed))
        .sorted(byElementName(idToElement)) // walk roots in predictable manner
        .map(fe -> createParameter(data, fe, allRiskElements))
        .forEach(
            parameter ->
                updateAllRootsInSubgraph(
                    unit, domain, listOfRootElements, processed, changedElements, parameter));

    // TODO: #2908 compare the number of nodes in the flyweight graph to the number of riskaffected
    // in the unit and clear those not in the flyweight
    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("updateAllRootNodes needed {} ms", timeNeeded);
    return changedElements;
  }

  private void updateAllRootsInSubgraph(
      Unit unit,
      Domain domain,
      List<FlyweightElement> listOfRootElements,
      Set<FlyweightElement> processed,
      ArrayList<Element> changedElements,
      UpdateAffectedGraphParameter parameter) {
    List<FlyweightElement> rootElementsForSubGraph =
        listOfRootElements.stream()
            .filter(notProcessed(processed))
            .filter(isPartOfGraph(parameter.elementSubGraph))
            .toList();

    processed.addAll(rootElementsForSubGraph);
    log.debug(
        "sub graph vertexes: {} edges:{} roots: {}",
        parameter.elementSubGraph.vertexSet().size(),
        parameter.elementSubGraph.edgeSet().size(),
        rootElementsForSubGraph.size());

    Set<RiskAffected<?, ?>> allRootsOfGraph =
        mapIdsToRiskAffected(parameter.riskAffectedCache, toIds(rootElementsForSubGraph));
    log.atDebug()
        .setMessage("all roots in subgraph: {} ")
        .addArgument(() -> listNodes(allRootsOfGraph))
        .log();
    allRootsOfGraph.stream()
        .sorted(comparing(Nameable::getName)) // walk roots in predictable manner
        .forEach(
            root -> {
              updateAffectedGraph(parameter, root, changedElements);
            });
  }

  @Override
  public Collection<Element> calculateImpactInheritance(
      Unit unit, Domain domain, String riskDefinitionId, RiskAffected<?, ?> affectedElement) {
    long startTime = System.currentTimeMillis();

    log.debug(
        "calculateImpactInheritance '{}' in domain '{}' and rd: '{}'",
        affectedElement.getName(),
        domain.getName(),
        riskDefinitionId);

    FlyweightImpactInheritenceContext data = prepareData(unit, domain, riskDefinitionId);
    if (!data.isInheritanceActive()) {
      return Collections.emptyList();
    }
    if (!data.hasValidGraph()) {
      affectedElement
          .getImpactValues(domain, data.definitionRef)
          .ifPresent(clearCalculatedValues(affectedElement));
      return Collections.emptyList();
    }

    // get the affected flyweightElement
    FlyweightElement flyweightElement =
        data.flyweightGraphElements.stream()
            .filter(t -> t.sourceId().equals(affectedElement.getIdAsString()))
            .findAny()
            .orElse(null);
    if (flyweightElement == null) {
      log.debug("affected element '{}' not in Graph", affectedElement.getName());
      affectedElement
          .getImpactValues(domain, data.definitionRef)
          .ifPresent(clearCalculatedValues(affectedElement));

      return Collections.emptyList();
    }

    UpdateAffectedGraphParameter parameter = createParameter(data, flyweightElement, null);
    if (parameter.detectCyclesContainingVertex(affectedElement)) {
      Set<Element> elementsInCycle = parameter.findCyclesContainingVertex(affectedElement);
      log.atDebug()
          .setMessage("{} is direct part of a cycle: {}")
          .addArgument(affectedElement.getName())
          .addArgument(() -> listNodes(elementsInCycle))
          .log();
      clearCalculatedImpactsInCycle(elementsInCycle, domain, data.definitionRef);
      return Collections.emptyList();
    }

    List<Element> changedElements = new ArrayList<>(parameter.elementGraph.vertexSet().size());
    updateAffectedGraph(parameter, affectedElement, changedElements);

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("calculateImpactInheritance took {} ms", timeNeeded);
    return changedElements;
  }

  /**
   * Walks the graph down by following the outgoing links, determine the max impact of the outgoing,
   * compare, update the calculated value and collect all elements affected.
   */
  private void updateAffectedGraph(
      UpdateAffectedGraphParameter graphData,
      RiskAffected<?, ?> affectedElement,
      List<Element> changedElements) {

    Set<CustomLink> outgoingEdges = graphData.elementGraph.outgoingEdgesOf(affectedElement);
    Set<CustomLink> incomingEdges = graphData.elementGraph.incomingEdgesOf(affectedElement);

    log.atDebug()
        .setMessage("updateAffectedGraph: {} incomingEdges: {} outgoingEdges: {}")
        .addArgument(affectedElement.getName())
        .addArgument(
            () -> incomingEdges.stream().map(this::toLinkName).collect(Collectors.joining(", ")))
        .addArgument(
            () -> outgoingEdges.stream().map(this::toLinkName).collect(Collectors.joining(", ")))
        .log();

    Map<CategoryRef, ImpactRef> maxImpactPerCategorie =
        getMaxImpactPerCategory(
            graphData.domain,
            graphData.riskDefinitionRef,
            incomingEdges,
            graphData.cats,
            graphData.elementGraph);
    Optional<ImpactValues> impactValues =
        affectedElement.getImpactValues(graphData.domain, graphData.riskDefinitionRef);
    impactValues.ifPresentOrElse(
        fillCalculatedImpacts(affectedElement, changedElements, maxImpactPerCategorie),
        initializeCalculatedImpacts(
            affectedElement,
            graphData.domain,
            graphData.riskDefinitionRef,
            changedElements,
            maxImpactPerCategorie));

    if (graphData.detectCyclesContainingVertex(affectedElement)) {
      Set<Element> elementsPartOfCycle = graphData.findCyclesContainingVertex(affectedElement);
      log.atDebug()
          .setMessage("{} is direct part of a cycle: {}")
          .addArgument(affectedElement.getName())
          .addArgument(() -> listNodes(elementsPartOfCycle))
          .log();
      clearCalculatedImpactsInCycle(
          elementsPartOfCycle, graphData.domain, graphData.riskDefinitionRef);
      // TODO: #2588 we could also return the already affected elements
      return;
    }

    // walk the graph down in a predictable manner
    outgoingEdges.stream()
        .map(e -> graphData.elementGraph.getEdgeTarget(e))
        .map(RiskAffected.class::cast)
        .sorted(comparing(Nameable::getName))
        .forEach(e -> updateAffectedGraph(graphData, e, changedElements));
  }

  /**
   * Breaks the complete graph down to the connected set which contains the affectedElement and
   * returns a new sub graph with the concrete elements.
   */
  private UpdateAffectedGraphParameter createParameter(
      FlyweightImpactInheritenceContext data,
      FlyweightElement affectedElement,
      Set<RiskAffected<?, ?>> riskAffectedCache) {

    Graph<FlyweightElement, FlyweightLink> elementSubGraph =
        new BiconnectivityInspector<>(data.completeGraph).getConnectedComponent(affectedElement);

    AbstractGraph<Element, CustomLink> elementGraph =
        buildElementGraph(
            data.domain, data.unit, data.inheritanceLinkTypes, elementSubGraph, riskAffectedCache);

    if (log.isTraceEnabled()) {
      //     export dot graph
      DOTExporter<Element, CustomLink> dotExporter = new DOTExporter<>();
      dotExporter.setVertexIdProvider(t -> "\"" + t.getName() + "\"");
      dotExporter.setEdgeIdProvider(t -> t.getType() + ":");
      StringWriter sw = new StringWriter();
      dotExporter.exportGraph(elementGraph, sw);
      log.debug("graph:\n{}", sw.toString());
    }

    return new UpdateAffectedGraphParameter(
        elementGraph,
        data.domain,
        data.catRefs,
        data.definitionRef,
        elementSubGraph,
        riskAffectedCache);
  }

  private FlyweightImpactInheritenceContext prepareData(
      Unit unit, Domain domain, String riskDefinitionId) {
    FlyweightImpactInheritenceContext fd = new FlyweightImpactInheritenceContext();
    fd.unit = unit;
    fd.domain = domain;
    if (!unit.getDomains().contains(domain)) {
      log.info(
          "Unit '{}'({}) not associated with domain '{}'",
          unit.getName(),
          unit.getIdAsString(),
          domain.getDisplayName());

      return fd;
    }
    Optional<RiskDefinition> ro = domain.getRiskDefinition(riskDefinitionId);
    if (ro.isEmpty()) {
      log.debug(
          "No risk definition for id: {} in domain '{}'",
          riskDefinitionId,
          domain.getDisplayName());
      return fd;
    }
    fd.riskDefinition = ro.get();
    fd.definitionRef = RiskDefinitionRef.from(fd.riskDefinition);

    fd.inheritanceLinkTypes = riskLinks(fd.riskDefinition);
    if (fd.inheritanceLinkTypes.isEmpty()) {
      log.debug(
          "no links defined for domain '{}' and risk definition '{}'",
          domain.getDisplayName(),
          riskDefinitionId);
      return fd;
    }
    fd.catRefs = fd.riskDefinition.getCategories().stream().map(c -> CategoryRef.from(c)).toList();
    if (fd.catRefs.isEmpty()) {
      log.debug("no Categories defined in {}", riskDefinitionId);
      return fd;
    }

    fd.flyweightGraphElements = loadFlyweightElements(unit, domain, fd.inheritanceLinkTypes);
    if (fd.flyweightGraphElements.isEmpty()) {
      log.debug("Not a connected Graph");
      return fd;
    }

    fd.completeGraph = buildFlyweightGraph(fd.flyweightGraphElements);

    log.debug(
        "full graph vertexes: {} edges:{}",
        fd.completeGraph.vertexSet().size(),
        fd.completeGraph.edgeSet().size());

    return fd;
  }

  /** Create the concrete Element/Link graph from a given flyweight graph. */
  private AbstractGraph<Element, CustomLink> buildElementGraph(
      Domain domain,
      Unit unit,
      Collection<String> inheritanceLinkTypes,
      Graph<FlyweightElement, FlyweightLink> elementSubGraph,
      Set<RiskAffected<?, ?>> riskAffectedCache) {
    Set<RiskAffected<?, ?>> riskAffectedElements =
        riskAffectedCache == null
            ? loadRiskElements(unit, domain, toIds(elementSubGraph.vertexSet()))
            : mapIdsToRiskAffected(riskAffectedCache, toIds(elementSubGraph.vertexSet()));
    Map<String, RiskAffected<?, ?>> elementById =
        riskAffectedElements.stream().collect(toMap(Identifiable::getIdAsString, identity()));

    DirectedPseudograph<Element, CustomLink> graph = new DirectedPseudograph<>(null, null, true);
    riskAffectedElements.forEach(graph::addVertex);
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
        flyweightGraphElements.stream().collect(toMap(FlyweightElement::sourceId, identity()));
    DirectedPseudograph<FlyweightElement, FlyweightLink> graph =
        new DirectedPseudograph<>(null, null, true);
    flyweightGraphElements.forEach(graph::addVertex);
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
            .map(maskSubgraph::getEdgeSource)
            .map(RiskAffected.class::cast)
            .map(ra -> ra.getImpactValues(domain, riskDefinitionRef))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ImpactValues::getPotentialImpactsEffective)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.groupingBy(Entry::getKey));

    Map<CategoryRef, ImpactRef> maxImpacts = new HashMap<>();
    for (CategoryRef categoryRef : catRefs) {
      incomingValues.getOrDefault(categoryRef, Collections.emptyList()).stream()
          .map(Entry::getValue)
          .max(comparing(ImpactRef::getIdRef))
          .ifPresent(i -> maxImpacts.put(categoryRef, ImpactRef.from(i)));
    }
    return maxImpacts;
  }

  private Set<FlyweightElement> loadFlyweightElements(
      Unit unit, Domain domain, Set<String> linkTypes) {
    long startTime = System.currentTimeMillis();
    Set<FlyweightElement> allLinksGroupedByElement =
        flyweightRepo.findAllLinksGroupedByElement(
            linkTypes, domain.getIdAsUUID(), unit.getIdAsUUID(), domain.getOwner().getIdAsUUID());

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("loadFlyweightElements in {} ms", timeNeeded);
    return allLinksGroupedByElement;
  }

  /** Loads all elements with in the given ids Set, uses unit and domain as a filter. */
  private Set<RiskAffected<?, ?>> loadRiskElements(Unit unit, Domain domain, Set<UUID> ids) {
    long startTime = System.currentTimeMillis();

    HashSet<RiskAffected<?, ?>> allElements = new HashSet<RiskAffected<?, ?>>(ids.size());
    allElements.addAll(queryElements(unit, domain, processRepository, ids));
    allElements.addAll(queryElements(unit, domain, assetRepository, ids));
    allElements.addAll(queryElements(unit, domain, scopeRepository, ids));

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("loadRiskElements: {} elements loaded in {} ms", allElements.size(), timeNeeded);
    return allElements;
  }

  /** Loads all elements in unit and domain as a filter. */
  private Set<RiskAffected<?, ?>> loadAllRiskElements(Unit unit, Domain domain) {
    long startTime = System.currentTimeMillis();

    HashSet<RiskAffected<?, ?>> allElements = new HashSet<RiskAffected<?, ?>>(200);
    allElements.addAll(queryElements(unit, domain, processRepository, null));
    allElements.addAll(queryElements(unit, domain, assetRepository, null));
    allElements.addAll(queryElements(unit, domain, scopeRepository, null));

    long timeNeeded = System.currentTimeMillis() - startTime;
    log.debug("loadRiskElements: {} elements loaded in {} ms", allElements.size(), timeNeeded);
    return allElements;
  }

  /** Queries the elements by ids. */
  private List<? extends RiskAffected<?, ?>> queryElements(
      Unit unit,
      Domain domain,
      RiskAffectedRepository<? extends RiskAffected<?, ?>, ?> repository,
      Set<UUID> ids) {
    ElementQuery<? extends RiskAffected<?, ?>> query = repository.query(unit.getClient());
    query.whereOwnerIs(unit);
    query.whereDomainsContain(domain);
    if (ids != null) {
      query.whereIdIn(new QueryCondition<>(ids));
    }
    query.fetchRiskValuesAspects();
    return query.execute(PagingConfiguration.UNPAGED).getResultPage().stream().toList();
  }

  private Set<RiskAffected<?, ?>> mapIdsToRiskAffected(
      Set<RiskAffected<?, ?>> allRiskElements, Set<UUID> ids) {
    return allRiskElements.stream()
        .filter(r -> ids.contains(r.getIdAsUUID()))
        .collect(Collectors.toSet());
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

  private Runnable initializeCalculatedImpacts(
      RiskAffected<?, ?> affectedElement,
      Domain domain,
      RiskDefinitionRef riskDefinitionRef,
      List<Element> changedElements,
      Map<CategoryRef, ImpactRef> maxImpactPerCategorie) {
    return () -> {
      log.debug("{} create calculated values {}", affectedElement.getName(), maxImpactPerCategorie);

      affectedElement.setImpactValues(
          domain,
          Map.of(
              riskDefinitionRef,
              new ImpactValues(new HashMap<>(), maxImpactPerCategorie, null, null)));
      changedElements.add(saveAffectedElement(affectedElement));
    };
  }

  private void clearCalculatedImpactsInCycle(
      Set<? extends Element> elementsInCycle, Domain domain, RiskDefinitionRef riskDefinitionRef) {
    log.atDebug()
        .setMessage("clear calculated impact in elements : {}")
        .addArgument(() -> listNodes(elementsInCycle))
        .log();
    elementsInCycle.stream()
        .map(RiskAffected.class::cast)
        .forEach(
            e -> e.getImpactValues(domain, riskDefinitionRef).ifPresent(clearCalculatedValues(e)));
  }

  private Consumer<ImpactValues> clearCalculatedValues(RiskAffected<?, ?> affectedElement) {
    return iv -> {
      iv.potentialImpactsCalculated().clear();
      saveAffectedElement(affectedElement);
    };
  }

  private Consumer<ImpactValues> fillCalculatedImpacts(
      RiskAffected<?, ?> affectedElement,
      List<Element> changedElements,
      Map<CategoryRef, ImpactRef> maxImpactPerCategorie) {
    return iv -> {
      log.debug("{} set calculated values {}", affectedElement.getName(), maxImpactPerCategorie);
      iv.potentialImpactsCalculated().clear();
      iv.potentialImpactsCalculated().putAll(maxImpactPerCategorie);
      changedElements.add(saveAffectedElement(affectedElement));
    };
  }

  private Comparator<? super FlyweightElement> byElementName(
      Map<String, RiskAffected<?, ?>> idToElement) {
    return (o1, o2) ->
        idToElement
            .get(o1.sourceId())
            .getName()
            .compareTo(idToElement.get(o2.sourceId()).getName());
  }

  private Predicate<? super FlyweightElement> isPartOfGraph(
      Graph<FlyweightElement, FlyweightLink> elementSubGraph) {
    return v -> elementSubGraph.containsVertex(v);
  }

  private Predicate<? super FlyweightElement> notProcessed(Set<FlyweightElement> processed) {
    return f -> !processed.contains(f);
  }

  private Set<String> riskLinks(RiskDefinition riskDefinition) {
    return riskDefinition.getImpactInheritingLinks().values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private String listNodes(Set<? extends Element> nodes) {
    return nodes.stream().map(Nameable::getName).collect(Collectors.joining(", "));
  }

  private Set<UUID> toIds(Collection<FlyweightElement> rootElementsForSubGraph) {
    return rootElementsForSubGraph.stream()
        .map(f -> f.sourceId())
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  private String toLinkName(CustomLink l) {
    return l.getSource().getName() + "->" + l.getTarget().getName();
  }
}
