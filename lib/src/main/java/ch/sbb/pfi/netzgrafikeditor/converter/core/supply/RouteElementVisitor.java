package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

public interface RouteElementVisitor {
    void visit(RouteStop routeStop);

    void visit(RoutePass routePass);
}
