package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import java.util.List;

public interface InfrastructureRepository {

    StopFacilityInfo getStopFacility(String stopId);

    List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitLineInfo transitLineInfo);

}
