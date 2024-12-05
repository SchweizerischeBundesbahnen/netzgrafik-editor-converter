package ch.sbb.pfi.netzgrafikeditor.converter.core.validation;

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

    REPLACE_WHITESPACE {
        @Override
        public boolean apply(NetworkGraphicValidator validator) {
            if (!validator.isValid()) {
                validator.replaceWhitespace();
            }
            return true;
        }
    },

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
