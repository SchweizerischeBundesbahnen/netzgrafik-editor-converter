package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Transition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Preprocess trainrun segments, since they are not ordered in NGE JSON export.
 */
@Slf4j
@AllArgsConstructor
class TrainrunBuilder {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, Transition> transitions;
    private final Map<Integer, Port> ports = new HashMap<>();

    @Getter
    private final Trainrun train;
    @Getter
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

    void build() {
        List<TrainrunSection> orderedSections = new LinkedList<>();

        for (TrainrunSection section : sections.values()) {
            log.info("Trainrun ID: {}, Source Node: {} (Stop: {}), Target Node: {} (Stop: {})", section.getTrainrunId(),
                    section.getSourceNodeId(), nodes.get(section.getSourceNodeId()).getBetriebspunktName(),
                    section.getTargetNodeId(), nodes.get(section.getTargetNodeId()).getBetriebspunktName());
        }

        TrainrunSection randomSection = sections.values().iterator().next();

        TrainrunSection startSection = findStart(randomSection);

        log.info("Start: {}, Source Node: {} (Stop: {}), Target Node: {} (Stop: {})", startSection.getTrainrunId(),
                startSection.getSourceNodeId(), nodes.get(startSection.getSourceNodeId()).getBetriebspunktName(),
                startSection.getTargetNodeId(), nodes.get(startSection.getTargetNodeId()).getBetriebspunktName());


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

    private TrainrunSection findStart(TrainrunSection section) {
        Map<Integer, TrainrunSection> sectionsToVisit = new HashMap<>(sections);
        boolean swapped = false;
        TrainrunSection current = section;
        TrainrunSection next;

        int count = 0;

        while (true) {

            // mark as visited; remove from sections to visit
            sectionsToVisit.remove(current.getId());

            // normal case; search on the left
            next = getSection(current, swapped ? SearchCase.TARGET_NODE : SearchCase.SOURCE_NODE, sectionsToVisit);

            // swapped case; search on the right
            if (next == null) {
                next = getSection(current, swapped ? SearchCase.SOURCE_NODE : SearchCase.TARGET_NODE, sectionsToVisit);
            }

            // nothing found; we are at the end of chain
            if (next == null) {
                break;
            }

            // check if section is swapped; source node of current must be target node of previous
            swapped = current.getSourceNodeId() != next.getTargetNodeId();

            // advance one section
            log.info("Swapped: {}, current: {}, next: {}", swapped, format(current), format(next));
            current = next;

            count++;
            if (count > 10) {
                throw new IllegalStateException("Abort...");
            }
        }

        return current;
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


    enum SearchCase {
        SOURCE_NODE,
        TARGET_NODE
    }


}
