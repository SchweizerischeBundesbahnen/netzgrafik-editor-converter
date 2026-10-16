package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.core.ServiceDayBoundary;
import lombok.extern.slf4j.Slf4j;

/**
 * Handling of the issues found in the network graphic.
 * <p>
 * The strategy applies to invalid IDs of nodes and trainruns and, with the service day boundary
 * {@link ServiceDayBoundary#DEPOT}, to lines without a stop labelled as depot, whose trips then only start and end at
 * the line termini. Missing depots cannot be repaired, so the repairing strategies only warn about them.
 */
@Slf4j
public enum ValidationStrategy {

    /**
     * Ignore all issues.
     */
    SKIP_VALIDATION {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            return true;
        }
    },

    /**
     * Log a warning for each issue and convert anyway.
     */
    WARN_ON_ISSUES {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            validator.isValid();
            return true;
        }
    },

    /**
     * Fail the conversion on any issue.
     */
    FAIL_ON_ISSUES {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            return validator.isValid();
        }
    },

    /**
     * Strip and replace whitespace in invalid IDs, and warn about the other issues.
     */
    REPLACE_WHITESPACE {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            if (!validator.isValid()) {
                validator.replaceWhitespace();
            }
            return true;
        }
    },

    /**
     * Remove special characters and replace whitespace in invalid IDs, and warn about the other issues.
     */
    REMOVE_SPECIAL_CHARACTERS {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            if (!validator.isValid()) {
                validator.removeSpecialCharacters();
            }
            return true;
        }
    },

    /**
     * @deprecated This option is deprecated because specialized treatment of the IDs should ideally be performed
     * upstream of the converter. This option is provided only to ensure backward compatibility with the old default
     * behavior. Users are encouraged to perform ID manipulations before invoking the converter, preferably directly in
     * the Network Graphic Editor (NGE).
     */
    @Deprecated REMOVE_DOTS_AND_REPLACE_WHITESPACE {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            if (!validator.isValid()) {
                validator.removeDotsReplaceWhitespace();
            }
            return true;
        }
    };

    public abstract boolean apply(NetworkGraphicValidator validator);
}
