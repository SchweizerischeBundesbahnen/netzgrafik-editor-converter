package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.GtfsSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestFile;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectedTrainrunVariantsIT {
    // Same provenance as Editor-native deletion: remaining sections retain same trainrun ID.
    private Path makeInput(boolean roundTrip, int fragments, boolean reorder) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode graphic = (ObjectNode) mapper.readTree(Files.readString(
                new TestFile("ng/cases","simple.json").getPath()));
        ArrayNode sections = (ArrayNode) graphic.get("trainrunSections");
        if (fragments == 2) {
            sections.remove(1); // remove middle B-C and leave A-B and C-D
        }
        if (reorder) {
            List<com.fasterxml.jackson.databind.JsonNode> copy = new java.util.ArrayList<>();
            sections.forEach(copy::add);
            Collections.reverse(copy);
            sections.removeAll();
            copy.forEach(sections::add);
        }
        for (var n : (ArrayNode) graphic.get("nodes")) {
            ObjectNode node = (ObjectNode)n;
            ((ArrayNode)node.get("transitions")).removeAll();
            if (fragments == 2) {
                ArrayNode ports = (ArrayNode)node.get("ports");
                for (int i=ports.size()-1;i>=0;i--) {
                    if (ports.get(i).get("trainrunSectionId").asInt()==4) ports.remove(i);
                }
            }
        }
        ((ObjectNode)((ArrayNode)graphic.get("trainruns")).get(0))
                .put("direction", roundTrip?"round_trip":"one_way");
        Path tmp=Files.createTempFile("nge-gap-edge-", ".json");
        mapper.writeValue(tmp.toFile(),graphic);
        return tmp;
    }
    private GtfsSchedule convert(Path input, boolean trainNames) throws Exception {
        RollingStockRepository stock = new NoRollingStockRepository();
        var builder = new GtfsSupplyBuilder(new NoInfrastructureRepository(),
                stock, new NoVehicleCircuitsPlanner(stock));
        final GtfsSchedule[] result=new GtfsSchedule[1];
        var cfg=NetworkGraphicConverterConfig.builder()
                .useTrainNamesAsIds(trainNames).serviceDayStart(ServiceDayTime.of(6,0,0))
                .serviceDayEnd(ServiceDayTime.of(8,0,0)).build();
        new NetworkGraphicConverter<>(cfg,new JsonFileReader(input),builder,s->result[0]=s).run();
        return result[0];
    }
    @Test
    void shouldPreserveBothDirectionsOfEachDisconnectedFragment() throws Exception {
        Path input=makeInput(true,2,false);
        try {
            GtfsSchedule gtfs=convert(input,true);
            var served=gtfs.getStopTimes().stream().map(s->s.getStopId()).collect(Collectors.toSet());
            assertTrue(served.containsAll(Set.of("A","B","C","D")));
            assertEquals(2,gtfs.getRoutes().size(),"two distinct line identities");
            assertEquals(8,gtfs.getTrips().size(),"two hourly departures, two fragments, both directions");
        } finally {Files.deleteIfExists(input);}
    }
    @Test
    void shouldPreserveThreeIndependentFragments() throws Exception {
        Path input=makeInput(false,3,false);
        try {
            GtfsSchedule gtfs=convert(input,true);
            assertEquals(3,gtfs.getRoutes().size(),"one route per physically disconnected segment");
            assertEquals(6,gtfs.getTrips().size(),"three fragments at two departure instants");
        } finally {Files.deleteIfExists(input);}
    }
    @Test
    void shouldPreserveCoverageWhenInputOrderChanges() throws Exception {
        for(boolean names: new boolean[]{true,false}) {
            Path original=makeInput(false,2,false),reversed=makeInput(false,2,true);
            try {
                GtfsSchedule a=convert(original,names),b=convert(reversed,names);
                Set<String> stopsA=a.getStopTimes().stream().map(s->s.getStopId()).collect(Collectors.toSet());
                Set<String> stopsB=b.getStopTimes().stream().map(s->s.getStopId()).collect(Collectors.toSet());
                assertEquals(Set.of("A","B","C","D"),stopsA);
                assertEquals(stopsA,stopsB);
                assertEquals(a.getTrips().size(),b.getTrips().size());
            } finally {Files.deleteIfExists(original);Files.deleteIfExists(reversed);}
        }
    }
}
