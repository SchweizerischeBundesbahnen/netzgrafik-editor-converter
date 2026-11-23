package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Connection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the connections drawn in the nodes of the network graphic into legs: from a trainrun arriving at the node on
 * the section of one port to a trainrun departing from it on the section of the other.
 * <p>
 * A connection applies in every direction both trainruns run: both ways between two round trips, as the network graphic
 * is drawn symmetrically, and with a one-way trainrun only the way it runs. The order of the two ports does not matter:
 * it is the order in which the connection was drawn, which the Netzgrafik-Editor neither shows nor keeps up to date
 * when a trainrun becomes one-way.
 */
@RequiredArgsConstructor
@Slf4j
class ConnectionResolver {

    private final Map<Integer, Node> nodes;
    private final Map<Integer, Port> ports;
    private final Map<Integer, TrainrunSection> sections;
    private final Map<Integer, Trainrun> trains;
    private final Map<Integer, EnumMap<RouteDirection, List<TrainrunSection>>> sequences;

    Resolution resolve() {
        List<Leg> legs = new ArrayList<>();
        List<Unresolved> unresolved = new ArrayList<>();

        for (Node node : nodes.values().stream().sorted(Comparator.comparingInt(Node::getId)).toList()) {
            for (Connection connection : node.getConnections()) {
                resolve(node, connection, legs, unresolved);
            }
        }

        return new Resolution(legs, unresolved);
    }

    private void resolve(Node node, Connection connection, List<Leg> legs, List<Unresolved> unresolved) {
        Port port1 = ports.get(connection.getPort1Id());
        Port port2 = ports.get(connection.getPort2Id());
        if (port1 == null || port2 == null) {
            log.warn("Connection {} at node {} refers to a port that does not exist, skipping", connection.getId(),
                    node.getBetriebspunktName());
            return;
        }

        TrainrunSection section1 = sections.get(port1.getTrainrunSectionId());
        TrainrunSection section2 = sections.get(port2.getTrainrunSectionId());
        Trainrun train1 = trains.get(section1.getTrainrunId());
        Trainrun train2 = trains.get(section2.getTrainrunId());
        if (train1.getId() == train2.getId()) {
            log.debug("Connection {} at node {} connects trainrun {} with itself, skipping", connection.getId(),
                    node.getBetriebspunktName(), train1.getName());
            return;
        }

        boolean fromPort1 = addLeg(node, connection, train1, section1, train2, section2, legs);
        boolean fromPort2 = addLeg(node, connection, train2, section2, train1, section1, legs);
        if (!fromPort1 && !fromPort2) {
            unresolved.add(new Unresolved(connection.getId(), node, train1.getId(), train2.getId()));
        }
    }

    /**
     * Adds the leg from the arriving to the departing trainrun, if both run in the direction it needs.
     */
    private boolean addLeg(Node node, Connection connection, Trainrun arrivingTrain, TrainrunSection arrivingSection, Trainrun departingTrain, TrainrunSection departingSection, List<Leg> legs) {
        Endpoint arriving = findVisit(arrivingTrain, arrivingSection, node, true);
        Endpoint departing = findVisit(departingTrain, departingSection, node, false);
        if (arriving == null || departing == null) {
            return false;
        }

        legs.add(new Leg(connection.getId(), node, arriving, departing));
        return true;
    }

    /**
     * Finds the visit of the node in the direction in which the trainrun arrives at it on the section (departs from it
     * on the section); visit i is the first node of section i in the direction, and visit i + 1 its last.
     */
    private Endpoint findVisit(Trainrun train, TrainrunSection section, Node node, boolean arriving) {
        for (Map.Entry<RouteDirection, List<TrainrunSection>> entry : sequences.get(train.getId()).entrySet()) {
            List<TrainrunSection> directedSections = entry.getValue();
            for (int i = 0; i < directedSections.size(); i++) {
                TrainrunSection directed = directedSections.get(i);
                if (directed.getId() != section.getId()) {
                    continue;
                }
                if (arriving && directed.getTargetNodeId() == node.getId()) {
                    return new Endpoint(train.getId(), entry.getKey(), i + 1);
                }
                if (!arriving && directed.getSourceNodeId() == node.getId()) {
                    return new Endpoint(train.getId(), entry.getKey(), i);
                }
            }
        }

        return null;
    }

    /**
     * The visit of a node by a trainrun in a direction, by its index in the visits of the direction.
     */
    record Endpoint(int trainrunId, RouteDirection direction, int visitIndex) {
    }

    /**
     * A connection in the direction a train changes in: from the arriving to the departing trainrun.
     */
    record Leg(int connectionId, Node node, Endpoint arriving, Endpoint departing) {
    }

    /**
     * A connection without a leg: neither trainrun arrives at the node where the other departs from it, e.g. two
     * one-way trainruns that both end there. The trainruns are in the order of the ports.
     */
    record Unresolved(int connectionId, Node node, int trainrunId1, int trainrunId2) {
    }

    /**
     * The legs of the connections, and the connections that have none.
     */
    record Resolution(List<Leg> legs, List<Unresolved> unresolved) {
    }

}
