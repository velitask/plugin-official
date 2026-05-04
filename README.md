# Velitask Official Plugin

[Русский](README_ru.md)

The official set of built-in indicators bundled with [Velitask](https://velitask.com): speedometer, geo map, charts, time, distance, slope, and more. This repository contains the **source code** of those indicators — published as a reference implementation and as a starting point for plugin authors who want to fork, modify, or learn from production-grade Velitask plugins.

## Quick start

```bash
git clone https://github.com/velitask/plugin-official
cd plugin-official
./gradlew jar
```

The resulting jar appears in `build/libs/velitask-official-indicators-1.0.0.jar`. To use a custom build of these indicators alongside or instead of the bundled set, drop it into your Velitask plugins folder:

```
~/.velitask/plugins/velitask-official-indicators-1.0.0.jar
```

Restart Velitask. Your custom build replaces the default in-app version.

## What's inside

- `src/main/java/com/velitask/plugin/official/` — indicator implementations (Speedometer, GeoMap, charts, time/distance/slope text indicators, video, etc.).
- `src/main/java/com/velitask/plugin/official/charts/` — chart-style indicators (millimeters, slope, speed).
- `src/main/java/com/velitask/plugin/official/figures/` — primitive figures (Line, Ellipse, Rectangle).
- `src/main/java/com/velitask/plugin/official/geo/` — geo-related figures (track, position).
- `src/main/resources/strings/` — localization (`strings.properties` + `strings_ru.properties`).
- `src/main/resources/svg/` — SVG assets used by visual indicators.

## How it depends on the SDK

```gradle
dependencies {
    compileOnly 'com.github.velitask:velitask-sdk:1.0.+'
}
```

The SDK is fetched from JitPack via the `https://jitpack.io` Maven repository. No extra setup needed.

## Make it your own

1. Pick the indicator(s) you want to modify; the rest can stay or be removed.
2. Rename the package if you plan to ship it as a separate plugin: `com.velitask.plugin.official` → your own (e.g. `com.mycompany.indicators`).
3. Update the `Velitask-Plugin-Class` attribute in `build.gradle` to point to your renamed plugin class.
4. Edit `strings/strings*.properties` for your localization keys.
5. `./gradlew jar` and drop the result into `~/.velitask/plugins/`.

## Localization check

```bash
./gradlew checkLocalization
```

Verifies that every `localized("key")` call in source code has a matching entry in `strings.properties` and `strings_ru.properties`.

## Documentation

- SDK API reference: https://javadoc.jitpack.io/com/github/velitask/velitask-sdk/latest/javadoc/
- SDK wiki & guides: https://github.com/velitask/velitask-sdk/wiki
- This plugin's wiki: https://github.com/velitask/plugin-official/wiki

## License

[MIT](LICENSE) — fork, modify, ship as your own, no attribution required.

The Velitask SDK itself is licensed under [Apache License 2.0](https://github.com/velitask/velitask-sdk/blob/main/LICENSE).
