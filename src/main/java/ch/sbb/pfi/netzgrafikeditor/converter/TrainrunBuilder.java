package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Transition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Traverse and order trainrun sections
 */
@Slf4j
@AllArgsConstructor
class TrainrunBuilder {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, Port> ports = new HashMap<>();
    private final Map<Integer, TrainrunSection> sections = new HashMap<>();

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

    /**
     * Traverse the sections and build an ordered sequence.
     */
    List<TrainrunSection> build() {
        List<TrainrunSection> orderedSections = new LinkedList<>(); // append to start of list

        // find start of the chain
        TrainrunSection randomSection = sections.values().iterator().next();
        AtomicReference<TrainrunSection> startSection = new AtomicReference<>();
        traverse(randomSection, startSection::set);

        // traverse from start section
        traverse(startSection.get(), orderedSections::addFirst);

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

        return orderedSections;
    }

    private void traverse(TrainrunSection root, Consumer<TrainrunSection> action) {
        Map<Integer, TrainrunSection> sectionsToVisit = new HashMap<>(sections);
        TrainrunSection current = root;
        TrainrunSection next;

        while (true) {

            // apply action on current section
            action.accept(current);

            // mark as visited; remove from sections to visit
            sectionsToVisit.remove(current.getId());

            // normal case; search on the target side (invert if source traversal)
            next = getNextSection(current, TraversalMode.TARGET, sectionsToVisit);

            // swapped case; search on source side (invert if source traversal)
            if (next == null) {
                next = getNextSection(current, TraversalMode.SOURCE, sectionsToVisit);
            }

            // nothing found; we are at the end of chain
            if (next == null) {
                break;
            }

            // advance one section
            current = next;
        }
    }

    private TrainrunSection getNextSection(TrainrunSection section, TraversalMode traversalMode, Map<Integer, TrainrunSection> sectionsToVisit) {

        // get node to search on
        Node node = switch (traversalMode) {
            case SOURCE -> nodes.get(section.getSourceNodeId());
            case TARGET -> nodes.get(section.getTargetNodeId());
        };

        // normal case: check if section occurs on source node
        for (Port port : node.getPorts()) {

            // search a port connecting to this trainrun section
            if (port.getTrainrunSectionId() == section.getId()) {

                // search for a transition
                for (Transition transition : node.getTransitions()) {

                    // transition connects on port 1, return next section on port 2
                    if (transition.getPort1Id() == port.getId()) {
                        return sectionsToVisit.get(ports.get(transition.getPort2Id()).getTrainrunSectionId());
                    }

                    // transition connects on port 2, return next section on port 1
                    if (transition.getPort2Id() == port.getId()) {
                        return sectionsToVisit.get(ports.get(transition.getPort1Id()).getTrainrunSectionId());
                    }
                }
            }
        }

        // no section found
        return null;

    }

    private enum TraversalMode {
        SOURCE,
        TARGET
    }


}
