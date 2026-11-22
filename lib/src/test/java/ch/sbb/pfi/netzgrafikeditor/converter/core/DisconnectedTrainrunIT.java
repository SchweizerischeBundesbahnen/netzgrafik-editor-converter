package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.GtfsSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs.GtfsScheduleWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestFile;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectedTrainrunIT {
    private Path makeEditorStyleHole() throws Exception {
        // Start from SBB's real JSON fixture, and apply the same delete-section operation as the Editor UI.
        ObjectMapper mapper = new ObjectMapper();
        var original = new TestFile("ng/cases", "simple.json");
        ObjectNode graphic = (ObjectNode) mapper.readTree(Files.readString(original.getPath()));
        // Original route A-B-C-D: delete middle B-C, keep A-B and C-D with same trainrun id.
        ((ArrayNode) graphic.get("trainrunSections")).remove(1);
        for (var node : (ArrayNode) graphic.get("nodes")) {
            var ports = (ArrayNode) node.get("ports");
            for (int i = ports.size()-1; i >= 0; i--) {
                if (ports.get(i).get("trainrunSectionId").asInt() == 4) ports.remove(i);
            }
            ((ArrayNode) ((ObjectNode) node).get("transitions")).removeAll();
        }
        ((ObjectNode) ((ArrayNode) graphic.get("trainruns")).get(0)).put("direction", "one_way");
        Path result = Files.createTempFile("nge-editor-hole-", ".json");
        mapper.writeValue(result.toFile(), graphic);
        return result;
    }

    private NetworkGraphicConverterConfig config() {
        return NetworkGraphicConverterConfig.builder().useTrainNamesAsIds(true)
                .serviceDayStart(ServiceDayTime.of(6,0,0))
                .serviceDayEnd(ServiceDayTime.of(8,0,0)).build();
    }

    @Test
    void shouldPreserveAllDisconnectedFragmentsInWrittenGtfs() throws Exception {
        Path input = makeEditorStyleHole();
        Path output = Files.createTempDirectory("nge-gap-gtfs-");
        try {
            RollingStockRepository stock = new NoRollingStockRepository();
            var builder = new GtfsSupplyBuilder(new NoInfrastructureRepository(),stock,new NoVehicleCircuitsPlanner(stock));
            final GtfsSchedule[] result = new GtfsSchedule[1];
            new NetworkGraphicConverter<>(config(), new JsonFileReader(input), builder, schedule -> {
                result[0]=schedule;
                new GtfsScheduleWriter(output, false).save(schedule);
            }).run();
            assertTrue(Files.exists(output.resolve("trips.txt")));
            assertTrue(Files.exists(output.resolve("stop_times.txt")));
            Set<String> served=result[0].getStopTimes().stream().map(s->s.getStopId()).collect(Collectors.toSet());
            assertTrue(served.containsAll(Set.of("A","B","C","D")),
                    "GTFS output lost at least one disconnected active service fragment");
        } finally { Files.deleteIfExists(input); }
    }

    @Test
    void shouldPreserveAllDisconnectedFragmentsInWrittenMatsim() throws Exception {
        Path input = makeEditorStyleHole();
        Path output = Files.createTempDirectory("nge-gap-matsim-");
        try {
            RollingStockRepository stock = new NoRollingStockRepository();
            var builder = new MatsimSupplyBuilder(new NoInfrastructureRepository(),stock,new NoVehicleCircuitsPlanner(stock));
            final Scenario[] result = new Scenario[1];
            new NetworkGraphicConverter<>(config(), new JsonFileReader(input), builder, scenario -> {
                result[0]=scenario;
                new TransitScheduleXmlWriter(output).save(scenario);
            }).run();
            long routes = result[0].getTransitSchedule().getTransitLines().values().stream()
                    .flatMap(l->l.getRoutes().values().stream()).count();
            assertTrue(Files.exists(output.resolve("transitSchedule.xml.gz")));
            assertEquals(2, routes, "MATSim output lost a whole active service fragment");
        } finally { Files.deleteIfExists(input); }
    }

    @Test
    void shouldKeepContinuousRoutesUnchanged() throws Exception {
        Path original = new TestFile("ng/cases","simple.json").getPath();
        RollingStockRepository stock = new NoRollingStockRepository();
        var builder = new GtfsSupplyBuilder(new NoInfrastructureRepository(),stock,new NoVehicleCircuitsPlanner(stock));
        final GtfsSchedule[] result = new GtfsSchedule[1];
        new NetworkGraphicConverter<>(config(),new JsonFileReader(original),builder,s->result[0]=s).run();
        Set<String> served=result[0].getStopTimes().stream().map(s->s.getStopId()).collect(Collectors.toSet());
        assertTrue(served.containsAll(Set.of("A","B","C","D")));
    }
}
