# Netzgrafik-Editor Converter

Converter to expand network graphics from
the [Netzgrafik-Editor](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend) into timetables for
the entire service day in different formats, as for example GTFS static or MATSim transit schedules.

## Usage

### Command line

Run the command line tool to convert a network graphic to either a GTFS or MATSim timetable:

```text
Usage: convert [-htV] [--non-passenger-network] [-b=<serviceDayBoundary>]
               [-d=<depotLabel>] [-e=<serviceDayEnd>] [-f=<outputFormat>]
               [-i=<stopFacilityCsv>] [-r=<rollingStockCsv>]
               [-s=<serviceDayStart>] [-v=<validationStrategy>]
               [--non-passenger-categories=<nonPassengerCategories>[,
               <nonPassengerCategories>...]]... <networkGraphicFile>
               <outputDirectory>

Converts network graphics into timetables in various formats.
      <networkGraphicFile>   The network graphic file to convert.
      <outputDirectory>      The output directory for the converted timetable.
  -b, --service-day-boundary=<serviceDayBoundary>
                             Handling of trips at the service day boundaries
                               (FULL, CLIP, DEPOT).
  -d, --depot-label=<depotLabel>
                             Node label which marks the depots in the network
                               graphic.
  -e, --service-day-end=<serviceDayEnd>
                             Service day end time (HH:mm).
  -f, --format=<outputFormat>
                             Output format (GTFS or MATSim).
  -h, --help                 Show this help message and exit.
  -i, --stop-facility-csv=<stopFacilityCsv>
                             File which contains the coordinates of the stop
                               facilities.
      --non-passenger-categories=<nonPassengerCategories>[,
        <nonPassengerCategories>...]
                             Trainrun categories without passenger service, as
                               comma-separated short names (e.g. G,GEX). Their
                               trains run, but cannot be boarded.
      --non-passenger-network
                             Convert only the trainruns without passenger
                               service, as a network of their own in which they
                               can be boarded, with transfers only at the drawn
                               connections (GTFS only).
  -r, --rolling-stock-csv=<rollingStockCsv>
                             File which contains the vehicle types to be mapped
                               to network graphic categories.
  -s, --service-day-start=<serviceDayStart>
                             Service day start time (HH:mm).
  -t, --train-names          Use train names as route or line IDs (true/false).
  -v, --validation=<validationStrategy>
                             Validation strategy (SKIP_VALIDATION,
                               WARN_ON_ISSUES, FAIL_ON_ISSUES,
                               REPLACE_WHITESPACE, REMOVE_SPECIAL_CHARACTERS).
  -V, --version              Print version information and exit.
```

Since the Netzgrafik-Editor does not provide information about the coordinates of nodes or rolling stock (vehicle types)
serving a category, this information can optionally be provided through CSV files.

Example:

```sh
# configure paths
NETWORK_GRAPHIC_FILE=../test/src/main/resources/ng/scenarios/realistic.json
OUTPUT_DIRECTORY=../integration-test/output/cmd

# output format: GTFS or MATSIM 
OUTPUT_FORMAT=GTFS

# optional CSV repositories
STOP_FACILITY_INFO_FILE=../test/src/main/resources/ng/scenarios/realistic-stop-facility-info.csv
ROLLING_STOCK_INFO_FILE=../test/src/main/resources/ng/scenarios/realistic-rolling-stock-info.csv

# run the Spring command line runner app to convert to GTFS / MATSim format
ARGS="$NETWORK_GRAPHIC_FILE $OUTPUT_DIRECTORY -f $OUTPUT_FORMAT -i $STOP_FACILITY_INFO_FILE -r $ROLLING_STOCK_INFO_FILE"
./mvnw spring-boot:run -pl app -Dspring-boot.run.arguments=$ARGS
```

### Service day boundary

The trainruns repeat with their frequency from their times in the Netzgrafik-Editor, independent of the service day
start. The service day boundary defines how trips at the start and end of the service day are handled:

| Boundary         | Trips                                                                             |
|------------------|-----------------------------------------------------------------------------------|
| `FULL` (default) | Run their full route if they depart from their first stop within the service day. |
| `CLIP`           | Are clipped to the stops served within the service day.                           |
| `DEPOT`          | Like `CLIP`, but start and end at depots only.                                    |

Depots are the nodes labelled `Depot` in the Netzgrafik-Editor (`--depot-label`) where a line stops; line termini always
count as depots.
See [Service Day Boundary](docs/service-day-boundary/service-day-boundary.md) for the exact rules, the handling of lines
without depot by the validation strategy, and time-distance diagrams.

### Connections

The connections drawn in the nodes of the Netzgrafik-Editor become GTFS transfers between the trips they link: a timed
transfer (`transfer_type` 1) from the arriving to the departing trip, which waits for it. With different frequencies,
only the trips that meet as drawn are connected. MATSim relates transfers to stop facilities only, so the connections
are left out there.

Trainruns without passenger service, such as freight trains, empty runs, or construction, rescue and shunting trains,
are marked by their category (`--non-passenger-categories`, e.g. `G,GEX`). Their trains run, but nobody boards them.
With `--non-passenger-network`, the converter writes these trainruns alone and lets them be boarded, as a GTFS network
in which a train changes to another trainrun only at a drawn connection, in no time.
See [Connections](docs/connections/connections.md) for the rules, the handling of connections that cannot be converted
as drawn by the validation strategy, and an example.

### Converter in Java

In most cases, the repositories for infrastructure, rolling stock, and vehicle circuits used by the supply builder will
be custom. To configure the converter with these custom repositories, inject them into the supply builder of the
selected timetable output format (refer to the Design section for details) and run the conversion:

```java

public class Example {

    public static final Path NETWORK_GRAPHIC_FILE = Path.of("path/to/networkGraphic.json");
    public static final Path OUTPUT_DIRECTORY = Path.of("path/to/outputDirectory");
    public static final NetworkGraphicConverterConfig CONFIG = NetworkGraphicConverterConfig.builder().build();

    static void main(String[] args) throws IOException {

        // define network graphic source
        NetworkGraphicSource source = new JsonFileReader(NETWORK_GRAPHIC_FILE);

        // instantiate custom implementations of the repositories
        RollingStockRepository customRollingStockRepository = new CustomRollingStockRepository();
        InfrastructureRepository customInfrastructureRepository = new CustomInfrastructureRepository();
        VehicleCircuitsPlanner customVehicleCircuitsPlanner = new CustomVehicleCircuitsPlanner(
                customRollingStockRepository);

        // convert to GTFS
        setupGtfsConverter(customInfrastructureRepository, customRollingStockRepository, customVehicleCircuitsPlanner,
                source).run();

        // convert to MATSim
        setupMatsimConverter(customInfrastructureRepository, customRollingStockRepository, customVehicleCircuitsPlanner,
                source).run();
    }

    private static NetworkGraphicConverter<Scenario> setupMatsimConverter(
            InfrastructureRepository customInfrastructureRepository, RollingStockRepository customRollingStockRepository,
            VehicleCircuitsPlanner customVehicleCircuitsPlanner, NetworkGraphicSource source) {
        SupplyBuilder<Scenario> builder = new MatsimSupplyBuilder(customInfrastructureRepository,
                customRollingStockRepository, customVehicleCircuitsPlanner, CONFIG.getValidationStrategy());
        ConverterSink<Scenario> sink = new TransitScheduleXmlWriter(Example.OUTPUT_DIRECTORY, "");

        return new NetworkGraphicConverter<>(CONFIG, source, builder, sink);
    }

    private static NetworkGraphicConverter<GtfsSchedule> setupGtfsConverter(
            InfrastructureRepository customInfrastructureRepository, RollingStockRepository customRollingStockRepository,
            VehicleCircuitsPlanner customVehicleCircuitsPlanner, NetworkGraphicSource source) {
        SupplyBuilder<GtfsSchedule> builder = new GtfsSupplyBuilder(customInfrastructureRepository,
                customRollingStockRepository, customVehicleCircuitsPlanner);
        ConverterSink<GtfsSchedule> sink = new GtfsScheduleWriter(Example.OUTPUT_DIRECTORY, true);

        return new NetworkGraphicConverter<>(CONFIG, source, builder, sink);
    }

}
```

## Design

The Maven project is structured into three modules: `app`, `lib` and `test`.

### Application

A command-line application based on Spring Boot's `CommandLineRunner`, which serves as the entry point for interacting
with the converter. The application module isolates the Spring Boot dependency.

### Library

The core functionality of the project is encapsulated in the library module, which provides the logic for reading,
converting, and writing network graphic data. The library has a modular design (DI):

- **converter**: Reads a network graphic from a source, converts it, and writes to a sink.
    - **core**: Converter logic and configuration.
        - **model**: DTOs to read network graphic data from JSON.
        - **supply**: A generic supply builder interface. Defines infrastructure and rolling stock repositories, as well
          as vehicle circuits planner interfaces used in the builder.
        - **validation**: Network graphic ID validator and sanitizer.
    - **adapter**: Format-specific transit schedule builder, implementing the supply builder interface.
    - **io**: Provides implementations for network graphic sources and converter output sinks.
    - **utils**: Utilities used across multiple domains.

The class diagram outlines core classes and their relationships:

![Class diagram](docs/uml/lib-class-diagram.svg)

### Testing

Enables access to test case files and provides a JUnit Jupiter Extension for easy integration test directory path
handling. Both the library and application modules depend on this module in the test scope.

## Contributing

This repository includes [Contribution Guidelines](CONTRIBUTING.md) that outline how to contribute to the project,
including how to submit bug reports, feature requests, and pull requests.
The [Coding Standards](CODING_STANDARDS.md) outline best practices and rules to follow when coding.
Details on releasing are described in the [Release](RELEASE.md) document.

## License

This project is licensed under GNU [GPL-3.0](LICENSE).

© 2024 Swiss Federal Railways SBB AG.
