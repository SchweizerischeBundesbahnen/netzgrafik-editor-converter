package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Identifiable;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.NetworkGraphic;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationUtils.*;

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
            log.warn("Invalid identifier: target={}, type={}, id={}", issue.target, issue.type, issue.id);
        }

        return issues.isEmpty();
    }

    private void validateId(String id, IssueTarget target, Identifiable object) {
        if (id == null || id.isEmpty()) {
            issues.add(new Issue<>(target, IssueType.MISSING, id, object));
            return;
        }

        if (containsSpecialCharacter(id)) {
            issues.add(new Issue<>(target, IssueType.SPECIAL_CHARACTER, id, object));
            return;
        }

        if (containsTrailingWhitespace(id)) {
            issues.add(new Issue<>(target, IssueType.LEADING_OR_TRAILING_WHITESPACE, id, object));
            return;
        }

        if (containsWhitespace(id)) {
            issues.add(new Issue<>(target, IssueType.WHITESPACE, id, object));
        }
    }

    void replaceWhitespace() {
        log.info("Strip and replace whitespace in invalid IDs of network graphic");
        networkGraphic = new NetworkGraphicSanitizer(networkGraphic, considerTrainruns,
                ValidationUtils::replaceWhitespace).run();
    }

    void removeSpecialCharacters() {
        log.info("Remove special characters in invalid IDs of network graphic");
        networkGraphic = new NetworkGraphicSanitizer(networkGraphic, considerTrainruns,
                s -> ValidationUtils.replaceWhitespace(ValidationUtils.removeSpecialCharacters(s))).run();
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

    private record Issue<T>(IssueTarget target, IssueType type, String id, T object) {
    }

}
