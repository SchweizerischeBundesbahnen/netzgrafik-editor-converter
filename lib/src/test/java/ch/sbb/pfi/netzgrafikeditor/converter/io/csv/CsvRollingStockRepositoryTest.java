package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvRollingStockRepositoryTest {

    private CsvRollingStockRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        repo = new CsvRollingStockRepository(TestScenario.REALISTIC_SCENARIO.getRollingStockInfoCsvFilePath());
    }

    @Test
    void getVehicleType() {
        VehicleTypeInfo vehicleTypeInfo = repo.getVehicleType("IC");
        assertEquals("FV-Dosto", vehicleTypeInfo.getId());
        assertEquals(600, vehicleTypeInfo.getSeats());
        assertEquals(55.56, vehicleTypeInfo.getMaxVelocity(), 0.01);
    }
}