package ch.sbb.pfi.netzgrafikeditor.converter.validation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ValidationStrategy {

    SKIP_VALIDATION {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            return true;
        }
    },

    WARN_ON_ISSUES {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            validator.isValid();
            return true;
        }
    },

    FAIL_ON_ISSUES {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            return validator.isValid();
        }
    },

    FIX_ISSUES {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            if (!validator.isValid()) {
                log.info("Fixing invalid IDs in network graphic");
                validator.fix();
            }
            return true;
        }
    };

    public abstract boolean apply(NetworkGraphicValidator validator);
}
