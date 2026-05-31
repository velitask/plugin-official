# Mixel system templates

These `.vttp` files are the **system-level Mixel templates** shipped by `plugin.official`. They get copied into `<install>/templates/` by the distribution pipeline (stage `plugin-official`) and act as the last-resort default Mixel layout when:

- the project folder has no `.vttp` for the source, AND
- the user templates folder has no `.vttp` for the source.

If the user removes these files, opening a source produces an empty Mixel + a notification asking to configure the layout manually or place a template.

## Files

| File | Used when |
|---|---|
| `video.vttp` | Default Mixel for VIDEO sources (mp4, mov) |
| `image.vttp` | Default Mixel for IMAGE sources (jpg, png, …) |
| `gpx.vttp` | Default Mixel for GPX sources |
| `thumbnail-video.vttp` | Thumbnail rendering for VIDEO sources in project browser |
| `thumbnail-image.vttp` | Thumbnail rendering for IMAGE sources in project browser |
| `thumbnail-gpx.vttp` | Thumbnail rendering for GPX sources in project browser |

File names match `SourceType.name().toLowerCase()` from the SDK
(`video`, `gpx`, `image`, `telemiger`). To add a default for a new source
type, create `<type>.vttp` in this folder.

## How to update

The current files are **MVP placeholders** with minimal layouts. Replace them with real Mixel snapshots:

1. Run Velitask, open a representative source.
2. Configure the Mixel layout (indicators, sizes, positions).
3. Use `File → Export Template` to save a `.vttp`.
4. Copy the exported file into this folder under the matching name.
5. Rebuild the distribution: `cd ../../../../distrib && ./gradlew rebuildStage -Pname=plugin-official && ./gradlew assembleRelease`.

## Format

`.vttp` is a full Mixel JSON snapshot produced by `mixel.toJSON(true)`. Layers reference indicators by their UID (`<plugin-uid>.<indicator-name>`). When loaded back via `mixel.fromJSON(json, true)`:

- Indicators not registered in the current `PlaginsManager` are silently skipped.
- `SensorProperty` / `SourceProperty` get `applyDefaultSource(...)` automatically — no need to bake source IDs into the template.
