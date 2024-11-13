package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Transition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Preprocess trainrun segments, since they are not ordered in NGE JSON export.
 */
@Slf4j
@AllArgsConstructor
class TrainrunBuilder implements Iterator<TrainrunSection> {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, Port> ports = new HashMap<>();
    private final Map<Integer, TrainrunSection> sections = new HashMap<>();

    @Getter
    private final Trainrun train;
    @Getter
    private final List<TrainrunSection> orderedSections = new ArrayList<>();
    @Getter
    private final List<Node> orderedNodes = new ArrayList<>();

    private static TrainrunSection swap(TrainrunSection original) {
        return TrainrunSection.builder().id(original.getId()) // keep
                .sourceNodeId(original.getTargetNodeId())  // swap
                .targetNodeId(original.getSourceNodeId())  // swap
                .trainrunId(original.getTrainrunId()) // keep
                .sourceArrival(original.getTargetArrival())  // swap
                .sourceDeparture(original.getTargetDeparture()) // swap
                .targetArrival(original.getSourceArrival()) // swap
                .targetDeparture(original.getSourceDeparture()) // swap
                .travelTime(original.getTravelTime()) // keep
                .build();
    }

    void add(TrainrunSection section) {
        sections.put(section.getId(), section);
        nodes.get(section.getSourceNodeId()).getPorts().forEach(port -> ports.put(port.getId(), port));
        nodes.get(section.getTargetNodeId()).getPorts().forEach(port -> ports.put(port.getId(), port));
    }

    void build() {
        for (TrainrunSection section : sections.values()) {
            log.info("Trainrun ID: {}, Source Node: {} (Stop: {}), Target Node: {} (Stop: {})", section.getTrainrunId(),
                    section.getSourceNodeId(), nodes.get(section.getSourceNodeId()).getBetriebspunktName(),
                    section.getTargetNodeId(), nodes.get(section.getTargetNodeId()).getBetriebspunktName());
        }

        // find start of chain
        TrainrunSection randomSection = sections.values().iterator().next();
        AtomicReference<TrainrunSection> startSection = new AtomicReference<>();
        iterateFromAndApply(randomSection, startSection::set);

        log.info("Start: {}, Source Node: {} (Stop: {}), Target Node: {} (Stop: {})",
                startSection.get().getTrainrunId(), startSection.get().getSourceNodeId(),
                nodes.get(startSection.get().getSourceNodeId()).getBetriebspunktName(),
                startSection.get().getTargetNodeId(),
                nodes.get(startSection.get().getTargetNodeId()).getBetriebspunktName());

        // start from initial section
        orderedSections.clear();
        iterateFromAndApply(startSection.get(), orderedSections::add);

        // swap first section if needed
        TrainrunSection first = orderedSections.getFirst();
        TrainrunSection second = orderedSections.get(1);
        if (first.getTargetNodeId() != second.getSourceNodeId() && first.getTargetNodeId() != first.getSourceNodeId()) {
            orderedSections.set(0, swap(first));
        }

        // swap other sections if needed
        for (int i = 1; i < orderedSections.size(); i++) {
            int previousTargetNodeId = orderedSections.get(i - 1).getTargetNodeId();
            TrainrunSection current = orderedSections.get(i);

            if (previousTargetNodeId != current.getSourceNodeId()) {
                orderedSections.set(i, swap(current));
            }
        }

        // add nodes
        orderedNodes.addFirst(nodes.get(orderedSections.getFirst().getSourceNodeId()));
        orderedSections.forEach(section -> orderedNodes.add(nodes.get(section.getTargetNodeId())));

        for (TrainrunSection section : orderedSections) {
            log.info("Ordered: {}", format(section));
        }
    }

    private String format(TrainrunSection section) {
        if (section == null) {
            return "-";
        } else {
            return String.format("%d: %s - %s", section.getId(),
                    nodes.get(section.getSourceNodeId()).getBetriebspunktName(),
                    nodes.get(section.getTargetNodeId()).getBetriebspunktName());
        }
    }

    private void iterateFromAndApply(TrainrunSection section, Consumer<TrainrunSection> action) {
        Map<Integer, TrainrunSection> sectionsToVisit = new HashMap<>(sections);
        TrainrunSection current = section;
        TrainrunSection next;

        while (true) {

            // apply action on current section
            action.accept(current);

            // mark as visited; remove from sections to visit
            sectionsToVisit.remove(current.getId());

            // normal case; search on the left
            next = getSection(current, SearchCase.SOURCE_NODE, sectionsToVisit);

            // swapped case; search on the right
            if (next == null) {
                next = getSection(current, SearchCase.TARGET_NODE, sectionsToVisit);
            }

            // nothing found; we are at the end of chain
            if (next == null) {
                break;
            }

            // advance one section
            log.info("current section: {}, next: {}", format(current), format(next));
            current = next;
        }
    }

    private TrainrunSection getSection(TrainrunSection section, SearchCase searchCase, Map<Integer, TrainrunSection> sectionsToVisit) {

        log.error("IN: {}", section.getId());

        // get node to search on
        Node node = switch (searchCase) {
            case SOURCE_NODE -> nodes.get(section.getSourceNodeId());
            case TARGET_NODE -> nodes.get(section.getTargetNodeId());
        };

        // normal case: check if section occurs on source node
        for (Port port : node.getPorts()) {

            // search a port connecting to this trainrun section
            if (port.getTrainrunSectionId() == section.getId()) {

                // search for a transition
                for (Transition transition : node.getTransitions()) {

                    // transition connects on port 1, return next section on port 2
                    if (transition.getPort1Id() == port.getId()) {
                        log.error("OUT: {}", ports.get(transition.getPort2Id()).getTrainrunSectionId());

                        return sectionsToVisit.get(ports.get(transition.getPort2Id()).getTrainrunSectionId());
                    }

                    // transition connects on port 2, return next section on port 1
                    if (transition.getPort2Id() == port.getId()) {
                        log.error("OUT: {}", ports.get(transition.getPort1Id()).getTrainrunSectionId());

                        return sectionsToVisit.get(ports.get(transition.getPort1Id()).getTrainrunSectionId());
                    }
                }
            }
        }

        log.error("OUT: null");

        // no section found
        return null;

    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public TrainrunSection next() {
        return null;
    }


    enum SearchCase {
        SOURCE_NODE,
        TARGET_NODE
    }


}
