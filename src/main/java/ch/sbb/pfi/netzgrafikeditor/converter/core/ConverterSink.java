package ch.sbb.pfi.netzgrafikeditor.converter.core;

import java.io.IOException;

public interface ConverterSink<T> {

    void save(T result) throws IOException;

}
