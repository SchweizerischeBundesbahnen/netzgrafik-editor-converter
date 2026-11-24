package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Transition;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Build a separate route for each connected part of a trainrun.
     * The editor retains disconnected parts, for example after a partial cancellation.
     */
    List<EnumMap<RouteDirection, List<TrainrunSection>>> buildComponents(Trainrun.Direction direction) {
        Map<Integer, TrainrunSection> remaining = new LinkedHashMap<>(sections);
        List<EnumMap<RouteDirection, List<TrainrunSection>>> components = new ArrayList<>();

        while (!remaining.isEmpty()) {
            List<TrainrunSection> orderedSections = new LinkedList<>();

            // find one end of this connected part
            TrainrunSection randomSection = remaining.values().iterator().next();
            AtomicReference<TrainrunSection> firstSection = new AtomicReference<>();
            traverse(randomSection, firstSection::set, remaining);

            // collect only this part and remove its sections from the remaining ones
            traverse(firstSection.get(), section -> {
                orderedSections.addFirst(section);
                remaining.remove(section.getId());
            }, remaining);

            SectionAligner.align(orderedSections);
            TrainrunSection first = orderedSections.getFirst();
            boolean drawnDirection = first.getSourceNodeId() == sections.get(first.getId()).getSourceNodeId();

            components.add(switch (direction) {
                case ROUND_TRIP -> new EnumMap<>(Map.of(RouteDirection.FORWARD, orderedSections,
                        RouteDirection.REVERSE, SectionAligner.reverse(orderedSections)));
                case ONE_WAY -> new EnumMap<>(Map.of(RouteDirection.FORWARD,
                        drawnDirection ? orderedSections : SectionAligner.reverse(orderedSections)));
            });
        }

        return components;
    }

    private void traverse(TrainrunSection root, Consumer<TrainrunSection> action,
                          Map<Integer, TrainrunSection> remaining) {
        Set<Integer> visited = new HashSet<>();
        TrainrunSection current = root;

        while (current != null) {
            action.accept(current);
            visited.add(current.getId());
            TrainrunSection next = getNextSection(current, TraversalMode.TARGET, remaining, visited);
            if (next == null) {
                next = getNextSection(current, TraversalMode.SOURCE, remaining, visited);
            }
            current = next;
        }
    }

    private TrainrunSection getNextSection(TrainrunSection section, TraversalMode traversalMode,
                                           Map<Integer, TrainrunSection> sectionsToVisit, Set<Integer> visited) {

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
                        int nextId = ports.get(transition.getPort2Id()).getTrainrunSectionId();
                        return visited.contains(nextId) ? null : sectionsToVisit.get(nextId);
                    }

                    // transition connects on port 2, return next section on port 1
                    if (transition.getPort2Id() == port.getId()) {
                        int nextId = ports.get(transition.getPort1Id()).getTrainrunSectionId();
                        return visited.contains(nextId) ? null : sectionsToVisit.get(nextId);
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

        private static List<TrainrunSection> reverse(List<TrainrunSection> sections) {
            return sections.reversed().stream().map(SectionAligner::swap).toList();
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
                    .travelTime(original.getBackwardTravelTime()) // swap
                    .backwardTravelTime(original.getTravelTime()) // swap
                    .build();
        }
    }

}
