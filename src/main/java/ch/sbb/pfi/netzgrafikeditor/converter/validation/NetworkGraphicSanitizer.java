package ch.sbb.pfi.netzgrafikeditor.converter.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Trainrun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.pfi.netzgrafikeditor.converter.validation.ValidationUtils.removeSpecialCharacters;
import static ch.sbb.pfi.netzgrafikeditor.converter.validation.ValidationUtils.removeWhitespace;

@RequiredArgsConstructor
@Slf4j
public class NetworkGraphicSanitizer {

    private final NetworkGraphic original;
    private final boolean considerTrainruns;

    public NetworkGraphic run() {

        List<Node> fixedNodes = new ArrayList<>();
        for (Node node : original.getNodes()) {
            String fixedBetriebspunktName = fixId(node.getBetriebspunktName());
            Node fixedNode = Node.builder()
                    .id(node.getId())
                    .betriebspunktName(fixedBetriebspunktName)
                    .fullName(node.getFullName())
                    .positionX(node.getPositionX())
                    .positionY(node.getPositionY())
                    .perronkanten(node.getPerronkanten())
                    .connectionTime(node.getConnectionTime())
                    .ports(node.getPorts())
                    .transitions(node.getTransitions())
                    .trainrunCategoryHaltezeiten(node.getTrainrunCategoryHaltezeiten())
                    .build();
            fixedNodes.add(fixedNode);
        }

        List<Trainrun> fixedTrainruns = original.getTrainruns();
        if (considerTrainruns) {
            fixedTrainruns = new ArrayList<>();
            for (Trainrun trainrun : original.getTrainruns()) {
                String fixedName = fixId(trainrun.getName());
                Trainrun fixedTrainrun = Trainrun.builder()
                        .id(trainrun.getId())
                        .name(fixedName)
                        .categoryId(trainrun.getCategoryId())
                        .frequencyId(trainrun.getFrequencyId())
                        .trainrunTimeCategoryId(trainrun.getTrainrunTimeCategoryId())
                        .build();
                fixedTrainruns.add(fixedTrainrun);
            }
        }

        return NetworkGraphic.builder()
                .nodes(fixedNodes)
                .trainrunSections(original.getTrainrunSections())
                .trainruns(fixedTrainruns)
                .metadata(original.getMetadata())
                .build();
    }

    private String fixId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Cannot fix id which is null or empty.");
        }

        return removeWhitespace(removeSpecialCharacters(id));
    }
}
