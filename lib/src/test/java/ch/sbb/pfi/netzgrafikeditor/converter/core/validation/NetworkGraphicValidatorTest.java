package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Trainrun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkGraphicValidatorTest {

    private NetworkGraphic original;

    private static List<String> getNodeIds(NetworkGraphic networkGraphic) {
        return networkGraphic.getNodes().stream().map(Node::getBetriebspunktName).toList();
    }

    private static List<String> getTrainrunIds(NetworkGraphic networkGraphic) {
        return networkGraphic.getTrainruns().stream().map(Trainrun::getName).toList();
    }

    @BeforeEach
    void setUp() {
        List<Node> nodes = List.of(Node.builder().betriebspunktName("validNode").build(),
                Node.builder().betriebspunktName("in validNode").build(),
                Node.builder().betriebspunktName(" invalidNode").build(),
                Node.builder().betriebspunktName("invalidNode ").build(),
                Node.builder().betriebspunktName("inVälid.Nöde").build());

        List<Trainrun> trainruns = List.of(Trainrun.builder().name("validTrainrun").build(),
                Trainrun.builder().name("in validTrainrun").build(),
                Trainrun.builder().name(" invalidTrainrun").build(),
                Trainrun.builder().name("invalidTrainrun ").build(),
                Trainrun.builder().name("inVälid.Träinrün").build());

        original = NetworkGraphic.builder().nodes(nodes).trainruns(trainruns).build();
    }

    @ParameterizedTest
    @EnumSource(ValidationStrategy.class)
    void run(ValidationStrategy strategy) {
        NetworkGraphicValidator validator = new NetworkGraphicValidator(strategy, true, original);

        switch (strategy) {
            case SKIP_VALIDATION, WARN_ON_ISSUES -> {
                NetworkGraphic validated = validator.run();
                assertEquals(original, validated);
            }
            case FAIL_ON_ISSUES -> assertThrows(IllegalStateException.class, validator::run);
            case REPLACE_WHITESPACE -> {
                NetworkGraphic validated = validator.run();
                assertEquals(List.of("validNode", "in_validNode", "invalidNode", "invalidNode", "inVälid.Nöde"),
                        getNodeIds(validated));
                assertEquals(List.of("validTrainrun", "in_validTrainrun", "invalidTrainrun", "invalidTrainrun",
                        "inVälid.Träinrün"), getTrainrunIds(validated));
            }
            case REMOVE_SPECIAL_CHARACTERS -> {
                NetworkGraphic validated = validator.run();
                assertEquals(List.of("validNode", "in_validNode", "invalidNode", "invalidNode", "inVlidNde"),
                        getNodeIds(validated));
                assertEquals(List.of("validTrainrun", "in_validTrainrun", "invalidTrainrun", "invalidTrainrun",
                        "inVlidTrinrn"), getTrainrunIds(validated));
            }
            case REMOVE_DOTS_AND_REPLACE_WHITESPACE -> {
                NetworkGraphic validated = validator.run();
                assertEquals(List.of("validNode", "in_validNode", "_invalidNode", "invalidNode_", "inVälidNöde"),
                        getNodeIds(validated));
                assertEquals(List.of("validTrainrun", "in_validTrainrun", "_invalidTrainrun", "invalidTrainrun_",
                        "inVälidTräinrün"), getTrainrunIds(validated));
            }
        }
    }
}