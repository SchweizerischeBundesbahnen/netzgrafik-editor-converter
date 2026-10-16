"""Generate the time-distance diagrams of the service day boundary documentation.

Usage: python3 diagrams.py (writes the SVG files next to this script)

The diagrams follow the model of the documentation: unroll the line, then cut each trip at the first depot departing
at or after the service day start and at the last depot arriving at or before the service day end.
"""
from pathlib import Path

# line A - F with the same running time on every section and a dwell time at the intermediate stops
NAMES, RUNNING, DWELL = "ABCDEF", 22, 6
ARRIVALS = [0] + [i * RUNNING + (i - 1) * DWELL for i in range(1, len(NAMES))]  # minutes after the departure at A
DEPARTURES_AT_STOP = [0] + [arrival + DWELL for arrival in ARRIVALS[1:-1]] + [ARRIVALS[-1]]
DEPARTURES = range(1 * 60 + 7, 26 * 60, 60)  # hourly at minute 07
START, END = 5 * 60, 25 * 60  # service day 05:00 - 01:00 next day
PANELS = [(3 * 60, 7 * 60), (22 * 60, 26 * 60)]  # morning and evening section shown

WIDTH, HEIGHT = 640, 820
LEFT, PLOT_WIDTH, TOP, GAP, SCALE = 90, 420, 110, 46, 1.25
COLOR_TRIP, COLOR_UNROLLED, COLOR_DAY, COLOR_BOUNDARY = "#c60018", "#a8a8a8", "#e8eef9", "#2d327d"

X = [LEFT + i * PLOT_WIDTH / (len(NAMES) - 1) for i in range(len(NAMES))]


def panel_top(panel):
    return TOP + sum((end - begin) * SCALE + GAP for begin, end in PANELS[:panel])


def y(minute, panel):
    return panel_top(panel) + (minute - PANELS[panel][0]) * SCALE


def hhmm(minute):
    return f"{minute // 60:02d}:{minute % 60:02d}"


def cut(departure, boundary, depots):
    """Get the first and last stop of the trip departing from A at the given time, or None if it is dropped."""
    last = len(NAMES) - 1
    if boundary == "FULL":
        return (0, last) if START <= departure < END else None
    first = [i for i in range(last + 1) if i in depots and departure + DEPARTURES_AT_STOP[i] >= START]
    final = [i for i in range(last + 1) if i in depots and departure + ARRIVALS[i] <= END]
    if not first or not final or min(first) >= max(final):
        return None
    return min(first), max(final)


def points(departure, first, last, panel):
    """Get the points of a trip from its departure at the first to its arrival at the last stop."""
    result = [(X[first], y(departure + DEPARTURES_AT_STOP[first], panel))]
    for i in range(first + 1, last + 1):
        result.append((X[i], y(departure + ARRIVALS[i], panel)))
        if i < last:
            result.append((X[i], y(departure + DEPARTURES_AT_STOP[i], panel)))
    return result


def polyline(points, color, width, dash=""):
    dash = f' stroke-dasharray="{dash}"' if dash else ""
    coordinates = " ".join(f"{px:.1f},{py:.1f}" for px, py in points)
    return f'<polyline points="{coordinates}" fill="none" stroke="{color}" stroke-width="{width}"{dash}/>'


def diagram(title, boundary, depots):
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" '
           f'viewBox="0 0 {WIDTH} {HEIGHT}" font-family="Helvetica, Arial, sans-serif" font-size="12">',
           f'<rect width="{WIDTH}" height="{HEIGHT}" fill="#ffffff"/>',
           '<pattern id="hatch" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
           '<line x1="0" y1="0" x2="0" y2="8" stroke="#d9d9d9" stroke-width="2"/></pattern>',
           f'<text x="{LEFT}" y="28" font-size="16" font-weight="bold">{title}</text>']

    # stops on top, depots filled
    for i, name in enumerate(NAMES):
        svg.append(f'<text x="{X[i]:.1f}" y="{TOP - 36}" text-anchor="middle" font-weight="bold">{name}</text>')
        fill = "#000000" if i in depots else "#ffffff"
        svg.append(f'<circle cx="{X[i]:.1f}" cy="{TOP - 18}" r="7" fill="{fill}" stroke="#000000" stroke-width="1.5"/>')

    for panel, (begin, end) in enumerate(PANELS):
        top, bottom = y(begin, panel), y(end, panel)
        svg.append(f'<clipPath id="panel{panel}"><rect x="{LEFT - 6}" y="{top}" width="{PLOT_WIDTH + 12}" '
                   f'height="{bottom - top}"/></clipPath>')

        # service day
        day_top, day_bottom = y(max(begin, START), panel), y(min(end, END), panel)
        svg.append(f'<rect x="{LEFT}" y="{day_top:.1f}" width="{PLOT_WIDTH}" height="{day_bottom - day_top:.1f}" '
                   f'fill="{COLOR_DAY}"/>')

        # time grid and stops as vertical lines
        for minute in range(begin + (-begin) % 60, end + 1, 60):
            svg.append(f'<line x1="{LEFT}" y1="{y(minute, panel):.1f}" x2="{LEFT + PLOT_WIDTH}" '
                       f'y2="{y(minute, panel):.1f}" stroke="#d9d9d9"/>')
            svg.append(f'<text x="{LEFT - 12}" y="{y(minute, panel) + 4:.1f}" text-anchor="end" fill="#555">'
                       f'{hhmm(minute)}</text>')
        for px in X:
            svg.append(f'<line x1="{px:.1f}" y1="{top:.1f}" x2="{px:.1f}" y2="{bottom:.1f}" stroke="#606060" '
                       f'stroke-width="1.2"/>')

        # service day start and end
        for minute, label in ((START, "service day start"), (END, "service day end")):
            if begin <= minute <= end:
                svg.append(f'<line x1="{LEFT}" y1="{y(minute, panel):.1f}" x2="{LEFT + PLOT_WIDTH}" '
                           f'y2="{y(minute, panel):.1f}" stroke="{COLOR_BOUNDARY}" stroke-width="2" '
                           f'stroke-dasharray="7 4"/>')
                svg.append(f'<text x="{LEFT + PLOT_WIDTH + 10}" y="{y(minute, panel) + 4:.1f}" '
                           f'fill="{COLOR_BOUNDARY}">{label}</text>')

        # unrolled trips and created trips
        svg.append(f'<g clip-path="url(#panel{panel})">')
        for departure in DEPARTURES:
            svg.append(polyline(points(departure, 0, len(NAMES) - 1, panel), COLOR_UNROLLED, 1.5, "4 3"))
            section = cut(departure, boundary, depots)
            if section:
                created = points(departure, *section, panel)
                svg.append(polyline(created, COLOR_TRIP, 3))
                for px, py in (created[0], created[-1]):
                    svg.append(f'<circle cx="{px:.1f}" cy="{py:.1f}" r="4" fill="{COLOR_TRIP}"/>')
        svg.append('</g>')

        # frame, dashed towards the break between the panels
        for x in (LEFT, LEFT + PLOT_WIDTH):
            svg.append(f'<line x1="{x}" y1="{top:.1f}" x2="{x}" y2="{bottom:.1f}" stroke="#606060"/>')
        for edge, inner in ((top, panel > 0), (bottom, panel < len(PANELS) - 1)):
            dash = ' stroke-dasharray="6 4"' if inner else ""
            svg.append(f'<line x1="{LEFT}" y1="{edge:.1f}" x2="{LEFT + PLOT_WIDTH}" y2="{edge:.1f}" '
                       f'stroke="#606060"{dash}/>')

    # break between the morning and the evening: hatched, with the stops and the frame continuing dashed
    gap_top, gap_bottom = y(PANELS[0][1], 0), panel_top(1)
    svg.append(f'<rect x="{LEFT}" y="{gap_top:.1f}" width="{PLOT_WIDTH}" height="{gap_bottom - gap_top:.1f}" '
               f'fill="url(#hatch)"/>')
    for px in [LEFT, *X, LEFT + PLOT_WIDTH]:
        svg.append(f'<line x1="{px:.1f}" y1="{gap_top:.1f}" x2="{px:.1f}" y2="{gap_bottom:.1f}" stroke="#606060" '
                   f'stroke-dasharray="4 4"/>')

    # legend
    legend = HEIGHT - 28
    svg += [f'<circle cx="{LEFT + 7}" cy="{legend}" r="7" fill="#000000" stroke="#000000" stroke-width="1.5"/>',
            f'<text x="{LEFT + 20}" y="{legend + 4}">depot</text>',
            f'<circle cx="{LEFT + 82}" cy="{legend}" r="7" fill="#ffffff" stroke="#000000" stroke-width="1.5"/>',
            f'<text x="{LEFT + 95}" y="{legend + 4}">stop</text>',
            polyline([(LEFT + 150, legend), (LEFT + 180, legend)], COLOR_UNROLLED, 1.5, "4 3"),
            f'<text x="{LEFT + 186}" y="{legend + 4}">unrolled trip</text>',
            polyline([(LEFT + 280, legend), (LEFT + 310, legend)], COLOR_TRIP, 3),
            f'<text x="{LEFT + 316}" y="{legend + 4}">created trip</text>',
            f'<rect x="{LEFT + 404}" y="{legend - 7}" width="24" height="14" fill="{COLOR_DAY}"/>',
            f'<text x="{LEFT + 434}" y="{legend + 4}">service day</text>',
            '</svg>']
    return "\n".join(svg) + "\n"


if __name__ == "__main__":
    termini = {0, len(NAMES) - 1}
    diagrams = {
        "full": ("FULL", "FULL", termini),
        "clip": ("CLIP", "CLIP", set(range(len(NAMES)))),
        "depot": ("DEPOT (depots at D and E)", "DEPOT", termini | {3, 4}),
        "depot-termini": ("DEPOT (no depot labelled, only the termini)", "DEPOT", termini),
    }
    target = Path(__file__).parent
    for name, (title, boundary, depots) in diagrams.items():
        (target / f"{name}.svg").write_text(diagram(title, boundary, depots))
