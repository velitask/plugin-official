# Официальный плагин Velitask

[English](README.md)

Набор встроенных индикаторов, поставляемых с [Velitask](https://velitask.com): спидометр, geo-карта, графики, время, дистанция, уклон и др. Этот репозиторий содержит **исходный код** индикаторов — публикуется как референсная реализация и стартовая точка для авторов плагинов, которые хотят форкнуть, модифицировать или изучить production-grade плагины Velitask.

## Быстрый старт

```bash
git clone https://github.com/velitask/plugin-official
cd plugin-official
./gradlew jar
```

Готовый jar появится в `build/libs/velitask-official-indicators-1.0.0.jar`. Чтобы использовать кастомную сборку индикаторов вместо или рядом с встроенным набором, положите jar в папку плагинов Velitask:

```
~/.velitask/plugins/velitask-official-indicators-1.0.0.jar
```

Перезапустите Velitask. Ваша сборка заместит встроенную версию.

## Что внутри

- `src/main/java/com/velitask/plugin/official/` — точка входа `VelitaskPlagin` и 14 классов индикаторов верхнего уровня (Speedometer, GeoMap, Compass, Video, графики Speed/Slope, текстовые time/distance/slope, примитивы Line/Ellipse/Rectangle).
- `src/main/java/com/velitask/plugin/official/charts/` — внутренние помощники для графических индикаторов.
- `src/main/java/com/velitask/plugin/official/geo/` — geo-фигуры (`GeoTrackFigure`, `GeoPositionFigure`), используются `GeoMapIndicator`.
- `src/main/java/com/velitask/plugin/official/slope/` — аналитика уклона (`SlopeDetector`, `SlopeGroup`, `SlopeGroupAtom`); вычисляется в `onSourceImported`, потребляется slope-индикаторами.
- `src/main/resources/strings/` — локализация (`strings.properties` + `strings_ru.properties`).
- `src/main/resources/svg/` — SVG-ассеты для визуальных индикаторов (компас, шкалы спидометра и т.д.).
- `src/main/resources/sql/setup/create.sql` — схема БД плагина (`slope_groups`, `slope_atom_groups`); SDK выполняет при первой установке плагина в проект.
- `src/main/resources/templates/` — system-level Mixel-шаблоны (`*.vttp`), используются как последний fallback при открытии источника без проектного и пользовательского шаблона. См. `templates/README.md`.

## Как зависит от SDK

```gradle
dependencies {
    compileOnly 'com.github.velitask:velitask-sdk:1.0.+'
}
```

SDK тянется с JitPack через Maven-репозиторий `https://jitpack.io`. Дополнительная настройка не нужна.

## Сделайте своим

1. Выберите индикатор(ы), которые хотите модифицировать; остальные оставьте или удалите.
2. Переименуйте пакет, если планируете шипнуть как отдельный плагин: `com.velitask.plugin.official` → ваш (например `com.mycompany.indicators`).
3. Поправьте атрибут `Velitask-Plugin-Class` в `build.gradle` — указать на ваш переименованный класс плагина.
4. Отредактируйте ключи в `strings/strings*.properties`.
5. `./gradlew jar` и положите результат в `~/.velitask/plugins/`.

## Проверка локализации

```bash
./gradlew checkLocalization
```

Проверяет, что каждый вызов `localized("key")` в исходниках имеет соответствующую запись в `strings.properties` и `strings_ru.properties`.

## Документация

- API SDK: https://javadoc.jitpack.io/com/github/velitask/velitask-sdk/latest/javadoc/
- Wiki SDK и гайды: https://github.com/velitask/velitask-sdk/wiki
- Wiki этого плагина: https://github.com/velitask/plugin-official/wiki

## Лицензия

[MIT](LICENSE) — форкайте, модифицируйте, шипите под своим именем, без атрибуции.

SDK Velitask лицензирован под [Apache License 2.0](https://github.com/velitask/velitask-sdk/blob/main/LICENSE).
