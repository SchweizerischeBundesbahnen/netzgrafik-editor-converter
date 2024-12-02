# Netzgrafik-Editor Converter

Converter to expand network graphics from
the [Netzgrafik-Editor](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend) into timetables for
the entire service
day in different formats, as for example GTFS static or MATSim transit schedules.

## Usage

Run the command line tool to convert a network graphic to either a GTFS or MATSim timetable:

```text
Usage: convert [-htV] [-e=<serviceDayEnd>] [-f=<outputFormat>]
               [-s=<serviceDayStart>] [-v=<validationStrategy>]
               <networkGraphicFile> <outputDirectory>
Converts network graphics into timetables in various formats.
      <networkGraphicFile>   The network graphic file to convert.
      <outputDirectory>      The output directory for the converted timetable.
  -e, --service-day-end=<serviceDayEnd>
                             Service day end time (HH:mm).
  -f, --format=<outputFormat>
                             Output format (GTFS or MATSim).
  -h, --help                 Show this help message and exit.
  -s, --service-day-start=<serviceDayStart>
                             Service day start time (HH:mm).
  -t, --train-names          Use train names as route or line IDs (true/false).
  -v, --validation=<validationStrategy>
                             Validation strategy (SKIP_VALIDATION,
                               WARN_ON_ISSUES, FAIL_ON_ISSUES, FIX_ISSUES).
  -V, --version              Print version information and exit.
```

Example:

```sh
# configure arguments
NETWORK_GRAPHIC_FILE=src/test/resources/ng/scenarios/realistic.json
OUTPUT_DIRECTORY=integration-test/output/cmd

# run the Spring command line runner app to convert to GTFS format
./mvnw spring-boot:run -Dspring-boot.run.arguments="$NETWORK_GRAPHIC_FILE $OUTPUT_DIRECTORY -f GTFS"

# run the Spring command line runner app to convert to MATSim format with custom service day times
./mvnw spring-boot:run -Dspring-boot.run.arguments="$NETWORK_GRAPHIC_FILE $OUTPUT_DIRECTORY -f MATSIM -s 04:30 -e 26:00"
```

## Design

The converter has a modular design (DI):

- **converter**: Reads a network graphic from a source, converts it, and writes to a sink.
    - **core**: Converter logic and configuration.
        - **model**: DTOs to read network graphic data from JSON.
        - **supply**: A generic supply builder interface. Defines infrastructure and rolling stock repositories, as well
          as vehicle circuits planner interfaces used in the builder.
        - **validation**: Network graphic ID validator and sanitizer.
    - **adapter**: Format-specific transit schedule builder, implementing the supply builder interface.
    - **io**: Provides implementations for network graphic sources and converter output sinks.

The class diagram outlines the core classes and their relationships:

![Class diagram](docs/uml/class-diagram.svg)

## License

This project is licensed under GNU [GPL-3.0](LICENSE).

© 2024 Swiss Federal Railways SBB AG.
