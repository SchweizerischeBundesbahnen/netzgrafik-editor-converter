package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.Identifiable;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
public class NetworkGraphicValidator {

    private final NetworkGraphic networkGraphic;
    private final boolean validateTrainruns;
    private final boolean failOnIssue;
    private final List<Issue<Identifiable>> issues = new ArrayList<>();

    void run() {

        networkGraphic.getNodes().forEach(node -> validate(node.getBetriebspunktName(), IssueTarget.NODE, node));

        if (validateTrainruns) {
            networkGraphic.getTrainruns()
                    .forEach(trainrun -> validate(trainrun.getName(), IssueTarget.TRAINRUN, trainrun));
        }

        for (Issue<Identifiable> issue : issues) {
            log.warn("Validation found issue: {}", issue);
        }

        if (!issues.isEmpty() && failOnIssue) {
            throw new IllegalStateException(
                    "Found issues during network graphic validation and option fail on issue is set.");
        }
    }

    private void validate(String id, IssueTarget target, Identifiable object) {
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

    private boolean containsSpecialCharacter(String input) {
        Pattern specialCharPattern = Pattern.compile("[^a-zA-Z0-9_\\-\\s]");
        return specialCharPattern.matcher(input).find();
    }

    private boolean containsWhitespace(String input) {
        return input.chars().anyMatch(Character::isWhitespace);
    }

    private boolean containsTrailingWhitespace(String input) {
        return !input.equals(input.strip());
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
