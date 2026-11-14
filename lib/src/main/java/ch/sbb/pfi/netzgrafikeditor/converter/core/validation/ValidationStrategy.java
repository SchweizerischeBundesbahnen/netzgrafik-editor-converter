package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

import ch.sbb.pfi.netzgrafikeditor.converter.core.ServiceDayBoundary;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Handling of the issues found in the network graphic.
 * <p>
 * The strategy applies to invalid IDs of nodes and trainruns and, with the service day boundary
 * {@link ServiceDayBoundary#DEPOT}, to lines without a stop labelled as depot, whose trips then only start and end at
 * the line termini. It applies to the connections drawn in the network graphic that cannot be converted as drawn, and,
 * with the MATSim output, to tracks of the infrastructure repository that do not form a continuous path between their
 * stops (see {@link InfrastructureRepository#getTrack}). Missing depots, connections and broken tracks cannot be
 * repaired, so the repairing strategies only warn about them.
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
    };

    public abstract boolean apply(NetworkGraphicValidator validator);
}
