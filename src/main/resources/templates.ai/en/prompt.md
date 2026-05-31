# Prompt for Creating Velitask HUD Templates for Video and Map

You are an assistant for creating `.vttp` JSON templates for Velitask.

I need to create a polished telemetry HUD template set in the style of a modern sports video editor:
1. a template for video;
2. a template for map view.

Work strictly based on my uploaded `.vttp` files.  
Do not invent the structure from scratch if my file already contains a working structure.  
First inspect my template, understand its layer format, `maket`, indicator properties, and only then make changes.

Documentation:
- Official Velitask indicator wiki:
  https://github.com/velitask/plugin-official/wiki
- Mixel layout and `maket` documentation:
  https://velitask.com/docs_ru.html#concepts-mixel-layouts

Important rules:
- Do not break the existing template structure.
- If the template already works, change it in small steps.
- Use `maket.preset: "proportional"` so the template scales properly across different resolutions.
- Create large visual blocks using `rectangle` as a wrapper.
- Indicators may be nested inside wrappers.
- Any indicator can act as a container if the template structure supports it.
- Do not add unverified fields that are not present in my working template or in the documentation.
- If you add a new indicator type, first use the minimal working structure.
- If a change may break the template, create a separate numbered file version.
- Name files sequentially: `filename_001.vttp`, `filename_002.vttp`, `filename_003.vttp`, and so on.

## Task for the Video Template

Create or improve a video template in a modern telemetry HUD style.

Layout:
- top left — time card;
- top right — distance card;
- bottom left — elevation card;
- bottom right — speedometer.

### Time Card

Create a dark semi-transparent `rectangle` wrapper with rounded corners and a clean cyan border.

Inside:
- a large but not too aggressive Unicode time icon on the left;
- a text column on the right:
  - title: `TIME`;
  - current time.

Use `timeText` for time.
Time format: `HH:mm:ss`.

### Distance Card

Create a dark semi-transparent `rectangle` wrapper with rounded corners and a border.

Inside:
- a large distance icon on the left;
- title: `DISTANCE`;
- a large current distance value;
- put the unit of measurement into a separate text layer so its size and color can be styled independently.

Secondary rows below:
- `TOTAL` — full distance;
- `MOVING` — moving time.

Secondary row style:
- icon on the left;
- label;
- value larger than the label;
- unit of measurement closer to the right edge;
- align all row elements visually along the bottom edge.

Use `distanceText` for distance:
- current distance: `{currNum}`;
- full distance: `{fullNum}`;
- number format: `%.1f`;
- units: `{currUnit}` / `{fullUnit}`, or separate `timeText` layers if separate styling is needed.

### Elevation Card

Create a dark semi-transparent `rectangle` wrapper.

Inside:
- title: `ELEVATION`;
- current elevation;
- rows:
  - `ASCENT` → `{up}`;
  - `DESCENT` → `{down}`;
  - `GRADE` → `{pc}`;
- put units into separate text layers: `M`, `%`.

Use `slopeText` for elevation data.
Important:
- use `{up}` for total ascent;
- use `{down}` for total descent;
- use `{pc}` for grade;
- use `{ele}` for current elevation.

Add thin divider lines between rows using `line`.
If you add an elevation graph, use `slopeChart` and place it inside the elevation card, not as a separate block.

### Speedometer

Add `speedometer`.
For video, place it in the lower right corner or in the lower part of the composition.
For map view, it should be neat and should not cover important parts of the route.

Requirements:
- display speed as an integer: `{curr}` with format `%.0f`;
- scale and digits should be light;
- arrow should be clean and not too acidic;
- arrow tail should be soft cyan/blue and not too bright;
- speedometer background should be transparent enough so it does not look heavy.

## Task for the Map Template

Create a separate `.vttp` template for the map based on my `map.vttp`.

Important:
- do not replace or recreate the map layer;
- keep `geoMap` as the main layer;
- add the HUD indicators on top of the map;
- if fonts become too large because of a different canvas size, scale them down proportionally;
- wrapper backgrounds on the map can be darker than in the video template so text remains readable;
- do not darken the entire map.

## Design Direction

Style:
- modern sports HUD;
- dark semi-transparent cards;
- rounded corners;
- thin cyan border;
- main values in white;
- labels and units in accent colors;
- header icons should not stand out too aggressively;
- icons can use the same accent color, but slightly dimmer or more transparent.

Colors:
- main accent: cyan / teal;
- additional colors:
  - green for ascent;
  - blue for descent;
  - orange / amber for total distance or grade;
- card background: dark but transparent.

## Result Check

After creating the file:
1. Make sure the JSON is valid.
2. Do not overwrite the original file; create a new `.vttp`.
3. Provide a download link for the new file.
4. Briefly list what changed.
5. If a new indicator may cause the template to fail, add it as a separate step and warn about the risk.
