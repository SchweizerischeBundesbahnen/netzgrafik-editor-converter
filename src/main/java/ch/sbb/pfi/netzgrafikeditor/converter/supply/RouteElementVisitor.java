package ch.sbb.pfi.netzgrafikeditor.converter.supply;

public interface RouteElementVisitor {
    void visit(RouteStop routeStop);

    void visit(RoutePass passInfo);
}
