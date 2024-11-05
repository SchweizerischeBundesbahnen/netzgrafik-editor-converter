package ch.sbb.pfi.netzgrafikeditor.converter;

import java.io.IOException;

public interface ConverterSink {

    void save() throws IOException;

}
