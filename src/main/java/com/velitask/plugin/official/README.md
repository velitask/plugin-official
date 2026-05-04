# com.velitask.plugin.official — официальный Velitask-плагин

Встроенные индикаторы Velitask (13 штук). Образец полноценного плагина на базе `com.velitask.plugin.sdk`.

План: [docs/plans/core-extraction/plugin-sdk-extraction/README.md](../../../../../../../../docs/plans/core-extraction/plugin-sdk-extraction/README.md).

## Правила

- Зависит только на `com.velitask.plugin.sdk.*`, `com.velitask.core.project.*`, `com.velitask.core.mixel.maket.*`, `org.abricos.*`, JDK.
- Не импортирует `com.velitask.core.runtime.*` (кроме случаев, идущих транзитивно через SDK).
