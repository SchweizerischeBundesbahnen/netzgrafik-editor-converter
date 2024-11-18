package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Transition;
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
class SectionSequenceBuilder {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, Port> ports = new HashMap<>();
    private final Map<Integer, TrainrunSection> sections = new HashMap<>();

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

        // find first section of the chain
        TrainrunSection randomSection = sections.values().iterator().next();
        AtomicReference<TrainrunSection> firstSection = new AtomicReference<>();
        traverse(randomSection, firstSection::set);

        // traverse from first section and collect sections
        traverse(firstSection.get(), orderedSections::addFirst);

        SectionAligner.align(orderedSections);

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

            // normal case; search on the target side
            next = getNextSection(current, TraversalMode.TARGET, sectionsToVisit);

            // swapped case; search on source side
            if (next == null) {
                next = getNextSection(current, TraversalMode.SOURCE, sectionsToVisit);
            }

            // nothing found; end of section sequence
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

        // iterate over ports on selected node
        for (Port port : node.getPorts()) {

            // check if port is connecting to this trainrun section
            if (port.getTrainrunSectionId() == section.getId()) {

                // search for a transition matching this port
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

    /**
     * Utility class for aligning and swapping train run sections.
     * <p>
     * The NGE records sections in the direction they are drawn. This class ensures that sections in a sequence are
     * correctly aligned by swapping the source and target nodes (and associated times) of sections when necessary.
     */
    private static class SectionAligner {

        private static void align(List<TrainrunSection> orderedSections) {

            // align segments if sequence is longer than one section
            if (orderedSections.size() > 1) {

                // check swap on first section
                TrainrunSection first = orderedSections.getFirst();
                TrainrunSection second = orderedSections.get(1);
                if (first.getTargetNodeId() != second.getSourceNodeId() && first.getTargetNodeId() != second.getTargetNodeId()) {
                    orderedSections.set(0, swap(first));
                }

                // check swaps on further sections
                for (int i = 1; i < orderedSections.size(); i++) {
                    int previousTargetNodeId = orderedSections.get(i - 1).getTargetNodeId();
                    TrainrunSection current = orderedSections.get(i);

                    if (previousTargetNodeId != current.getSourceNodeId()) {
                        orderedSections.set(i, swap(current));
                    }
                }
            }
        }

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
    }

}
