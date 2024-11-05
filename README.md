# Netzgrafik-Editor MATSim Converter

This tool converts network graphics from
the [Netzgrafik-Editor](https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend) into MATSim transit
schedules.

The converter has a modular design (DI):

- **converter**: Reads a network graphic from a source, converts it, and writes to a sink.
    - **model**: DTOs to read network graphic data from JSON.
    - **supply**: A generic supply builder interface. Defines infrastructure and rolling stock repositories, as well as
      vehicle circuits planner interfaces used in the builder.
    - **matsim**: MATSim-specific transit schedule builder, implementing the supply builder interface.
    - **io**: Provides implementations for network graphic sources and converter output sinks.

## License

This project is licensed under [Apache 2.0](LICENSE).
