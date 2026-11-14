package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Trainrun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class NetworkGraphicSanitizer {

    private final NetworkGraphic original;
    private final boolean considerTrainruns;
    private final Function<String, String> idProcessor;

    public NetworkGraphic run() {

        // copy every other field as is, so that none is lost when the model grows
        List<Node> fixedNodes = new ArrayList<>();
        for (Node node : original.getNodes()) {
            fixedNodes.add(node.toBuilder().betriebspunktName(process(node.getBetriebspunktName())).build());
        }

        List<Trainrun> fixedTrainruns = original.getTrainruns();
        if (considerTrainruns) {
            fixedTrainruns = new ArrayList<>();
            for (Trainrun trainrun : original.getTrainruns()) {
                fixedTrainruns.add(trainrun.toBuilder().name(process(trainrun.getName())).build());
            }
        }

        return NetworkGraphic.builder()
                .nodes(fixedNodes)
                .trainrunSections(original.getTrainrunSections())
                .trainruns(fixedTrainruns)
                .metadata(original.getMetadata())
                .labels(original.getLabels())
                .build();
    }

    private String process(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Cannot process id which is null or empty.");
        }

        return idProcessor.apply(id);
    }
}
