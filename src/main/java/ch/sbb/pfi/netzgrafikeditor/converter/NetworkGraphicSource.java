package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;

import java.io.IOException;

public interface NetworkGraphicSource {

    NetworkGraphic load() throws IOException;

}
