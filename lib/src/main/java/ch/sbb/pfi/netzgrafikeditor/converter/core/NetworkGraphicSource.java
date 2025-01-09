package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.NetworkGraphic;

import java.io.IOException;

public interface NetworkGraphicSource {

    NetworkGraphic load() throws IOException;

}
