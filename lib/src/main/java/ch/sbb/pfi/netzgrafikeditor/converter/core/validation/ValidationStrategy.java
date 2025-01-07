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
