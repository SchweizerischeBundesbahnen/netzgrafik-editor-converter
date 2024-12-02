# Netzgrafik-Editor Converter

Converter to expand network graphics from
the [Netzgrafik-Editor](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend) into timetables for
the entire service
day in different formats, as for example GTFS static or MATSim transit schedules.

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

## Usage

```sh
# configure arguments
NETWORK_GRAPHIC_FILE=src/test/resources/ng/scenarios/realistic.json
OUTPUT_DIRECTORY=integration-test/output/

# run spring command line runner app
./mvnw spring-boot:run -Dspring-boot.run.arguments="$NETWORK_GRAPHIC_FILE $OUTPUT_DIRECTORY"
```

## License

This project is licensed under GNU [GPL-3.0](LICENSE).

© 2024 Swiss Federal Railways SBB AG.
