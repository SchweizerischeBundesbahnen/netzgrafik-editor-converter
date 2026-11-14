package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Whether passengers can board and alight at a stop of a transit route.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum StopAccess {

    BOARDING_AND_ALIGHTING(true, true),
    BOARDING_ONLY(true, false),
    ALIGHTING_ONLY(false, true),
    NONE(false, false);

    private final boolean boarding;
    private final boolean alighting;

    public static StopAccess of(boolean boarding, boolean alighting) {
        if (boarding) {
            return alighting ? BOARDING_AND_ALIGHTING : BOARDING_ONLY;
        }
        return alighting ? ALIGHTING_ONLY : NONE;
    }

}
