package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the GTFS transfer_type, with the values of the official GTFS Static specification.
 *
 * @see <a href="https://gtfs.org/documentation/schedule/reference/#transferstxt">GTFS transfers.txt Documentation</a>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum TransferType {

    RECOMMENDED(0),
    TIMED(1),
    MINIMUM_TIME(2),
    NOT_POSSIBLE(3),
    IN_SEAT(4),
    IN_SEAT_NOT_ALLOWED(5);

    private final int value;

}
