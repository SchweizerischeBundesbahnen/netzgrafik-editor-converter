package ch.sbb.pfi.netzgrafikeditor.converter.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.model.Identifiable;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.pfi.netzgrafikeditor.converter.validation.ValidationUtils.*;

@AllArgsConstructor
@Slf4j
public class NetworkGraphicValidator {

    private final List<Issue<Identifiable>> issues = new ArrayList<>();

    private final ValidationStrategy strategy;
    private final boolean considerTrainruns;
    private NetworkGraphic networkGraphic;

    public NetworkGraphic run() {
        log.info("Apply validation strategy: {}", strategy);

        if (!strategy.apply(this)) {
            throw new IllegalStateException(
                    "Found issues during network graphic validation and option fail on issue is set.");
        }

        return networkGraphic;
    }

    boolean isValid() {
        networkGraphic.getNodes().forEach(node -> validateId(node.getBetriebspunktName(), IssueTarget.NODE, node));

        if (considerTrainruns) {
            networkGraphic.getTrainruns()
                    .forEach(trainrun -> validateId(trainrun.getName(), IssueTarget.TRAINRUN, trainrun));
        }

        for (Issue<Identifiable> issue : issues) {
            log.warn("Validation found issue: {}", issue);
        }

        return issues.isEmpty();
    }

    private void validateId(String id, IssueTarget target, Identifiable object) {
        if (id == null || id.isEmpty()) {
            issues.add(new Issue<>(target, IssueType.MISSING, object));
            return;
        }

        if (containsSpecialCharacter(id)) {
            issues.add(new Issue<>(target, IssueType.SPECIAL_CHARACTER, object));
            return;
        }

        if (containsTrailingWhitespace(id)) {
            issues.add(new Issue<>(target, IssueType.LEADING_OR_TRAILING_WHITESPACE, object));
            return;
        }

        if (containsWhitespace(id)) {
            issues.add(new Issue<>(target, IssueType.WHITESPACE, object));
        }
    }

    void fix() {
        networkGraphic = new NetworkGraphicSanitizer(networkGraphic, considerTrainruns).run();
    }

    private enum IssueTarget {
        NODE,
        TRAINRUN
    }

    private enum IssueType {
        MISSING,
        SPECIAL_CHARACTER,
        WHITESPACE,
        LEADING_OR_TRAILING_WHITESPACE
    }

    private record Issue<T>(IssueTarget target, IssueType type, T object) {
    }

}
