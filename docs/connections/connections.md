# Connections

A connection is drawn in a node of the Netzgrafik-Editor as a grey line between the ports of two trainruns. The
converter turns it into transfers between the trips it links, and can write the trainruns without passenger service as
a network of their own, in which a train changes to another trainrun only where a connection is drawn.

## Model

A connection joins the ports of two trainruns in a node. A train changes from the trainrun that arrives on the section
of one port to the trainrun that departs on the section of the other, in every direction both trainruns run: both ways
between two round trips, as the network graphic is drawn symmetrically, and with a one-way trainrun only the way it
runs. The order of the two ports does not matter. It is the order in which the connection was drawn, which the
Netzgrafik-Editor does not show, and which it does not adapt when a trainrun becomes one-way.

The **slack** of a connection is the time from the arrival to the nearest departure of the other trainrun. Both
trainruns repeat with their frequency, so the nearest departure is taken within their common period (the greatest
common divisor of the frequencies): a departure up to half the period before the arrival is a missed connection, not a
wait of almost a period.

Each trip arriving at the node is connected to the trip departing exactly the slack later. If the trainruns run with
different frequencies, only the trips that meet as drawn are connected: if trainrun A runs every 30 minutes and
trainrun B every 60 minutes, every other trip of A connects to B, and the others have no partner, as in the network
graphic. Routes clipped at the service day boundary take part like full routes.

## Output

| Format | Connection                                                                                                        |
|--------|-------------------------------------------------------------------------------------------------------------------|
| GTFS   | A row of `transfers.txt` from the arriving to the departing trip at the node, `transfer_type` 1 (timed transfer). |
| MATSim | Left out: a MATSim transit schedule relates transfer times to stop facilities only, not to particular trips.      |

A timed transfer means that the departing vehicle waits for the arriving one. GTFS ranks a transfer that names both
trips above one between the stops alone, so the connection holds even where the stop requires more time to change.

```text
from_stop_id,to_stop_id,from_route_id,to_route_id,from_trip_id,to_trip_id,transfer_type,min_transfer_time
B,B,,,IC_A_B_FORWARD_1,IC_B_C_FORWARD_1,1,
```

## Passenger and non-passenger service

The network graphic does not tell trainruns with passenger service from others, such as freight trains, empty runs, or
construction, rescue and shunting trains. The converter marks the trainruns without passenger service by their category
(`--non-passenger-categories`, e.g. `G,GEX`). Their trains run, so they still use capacity in a simulation, but nobody
boards them:

| Format | Stops of a trainrun without passenger service             |
|--------|-----------------------------------------------------------|
| GTFS   | `pickup_type` 1 and `drop_off_type` 1 at every stop time. |
| MATSim | `allowBoarding` and `allowAlighting` false at every stop. |

A connection of a trainrun without passenger service is left out.

## Non-passenger network

With `--non-passenger-network`, the converter writes the trainruns without passenger service alone, as the network
that is routed, e.g. to find the train paths a freight train can take. A train does not change trains there: it changes
to another train path at a node by setting the points, without stopping and in no time. So a train changes to another
trainrun only where a connection is drawn:

1. Only the trainruns without passenger service are written, and they can be boarded, as they are what is routed.
2. A trainrun starts and ends without dwell time at its first and last node: its arrival equals its departure.
3. Where a connection attaches to a trainrun that passes the node without stopping, the trainrun gets a **technical
   stop** there, with its arrival equal to its departure. Only the side the connection needs is open: alighting only
   where the trainrun arrives for the change, boarding only where it departs after the change (`pickup_type` and
   `drop_off_type` 1 on the other side). The other passes stay without a stop.
4. Every stop forbids transfers (`transfer_type` 3 from the stop to itself). The connections are written as above and
   override this for the trips they link.

The network needs transfers between particular trips, so only GTFS supports it; the conversion to MATSim fails.

### Example

Freight trainrun G1 runs from D to B and arrives there at 06:20. Freight trainrun G2 runs from E via B to C and passes B
at 06:20 without stopping. A connection is drawn from G1 to G2 at B, and both are round trips:

```text
stop_times.txt
trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type
G_D_B_FORWARD_1,06:20:00,06:20:00,B,2,,
G_E_C_FORWARD_1,06:20:00,06:20:00,B,2,,1
G_E_C_REVERSE_1,06:40:00,06:40:00,B,2,1,
G_D_B_REVERSE_1,06:40:00,06:40:00,B,1,,

transfers.txt
from_stop_id,to_stop_id,from_route_id,to_route_id,from_trip_id,to_trip_id,transfer_type,min_transfer_time
B,B,,,G_D_B_FORWARD_1,G_E_C_FORWARD_1,1,
B,B,,,G_E_C_REVERSE_1,G_D_B_REVERSE_1,1,
B,B,,,,,3,
```

G2 stops at B in no time: in its forward direction a train boards it there after arriving with G1, in its reverse
direction a train alights there to continue with G1. No other transfer is possible at B.

## Validation

A connection that cannot be converted as drawn is an issue, handled according to the validation strategy
(`--validation`). Whatever the strategy, the converter writes only what can be written.

| Issue                   | When                                                                                                           | Written                        |
|-------------------------|----------------------------------------------------------------------------------------------------------------|--------------------------------|
| `MISSED`                | The slack is negative: the departing train leaves before the arriving train arrives.                           | No                             |
| `BELOW_CONNECTION_TIME` | Passenger service: the slack is shorter than the connection time of the node, the Netzgrafik-Editor's warning. | Yes, the departing train waits |
| `AT_PASS`               | Passenger service: a trainrun of the connection passes the node without stopping.                              | No                             |
| `TO_EXCLUDED_TRAINRUN`  | Non-passenger network: the other trainrun of the connection has passenger service, so it is left out.          | No                             |
| `WITHOUT_TRIPS`         | No trip arriving has a trip departing the slack later within the service day.                                  | Nothing to write               |
| `AGAINST_DIRECTION`     | Neither trainrun arrives at the node where the other departs from it, e.g. two one-way trainruns ending there. | No                             |

| Strategy                                          | Issues                                                                             |
|---------------------------------------------------|------------------------------------------------------------------------------------|
| `SKIP_VALIDATION`                                 | Logged at debug level.                                                             |
| `WARN_ON_ISSUES`                                  | One warning per issue.                                                             |
| `FAIL_ON_ISSUES`                                  | One warning per issue, then the conversion fails.                                  |
| `REPLACE_WHITESPACE`, `REMOVE_SPECIAL_CHARACTERS` | One warning per issue: a connection cannot be repaired, as a missing depot cannot. |
