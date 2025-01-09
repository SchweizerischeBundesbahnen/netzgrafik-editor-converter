package ch.sbb.pfi.netzgrafikeditor.converter.app;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import picocli.CommandLine;

public class ServiceDayTimeConverter implements CommandLine.ITypeConverter<ServiceDayTime> {

    @Override
    public ServiceDayTime convert(String value) {
        String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:mm.");
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return ServiceDayTime.of(hours, minutes, 0);
    }
}
