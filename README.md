# CustomTNTFlow

CustomTNTFlow — плагин для Paper 1.21.1+, позволяющий собирать TNT с произвольными визуальными и поведенческими пресетами, управлять взрывами через конфиг/миксины и реагировать на них из других плагинов через стабильное API и собственные события.【F:build.gradle.kts†L21-L25】【F:src/main/java/dev/byflow/customtntflow/CustomTNTFlowPlugin.java†L22-L52】

## Быстрый старт

* Требования: Java 21, сервер Paper 1.21.1 (или совместимый форк).【F:build.gradle.kts†L8-L31】
* Сборка: `gradle build`. Готовый JAR появится в `build/libs`, а задача `copyPlugin` скопирует его в `build/output/` для ручной установки.【F:build.gradle.kts†L21-L45】
* После первого запуска в `plugins/CustomTNTFlow/` появится `config.yml` с образцами типов и миксинов.【F:src/main/java/dev/byflow/customtntflow/CustomTNTFlowPlugin.java†L26-L35】【F:src/main/resources/config.yml†L17-L143】

## Основные возможности

* Любое число типов TNT с настраиваемыми предметами (материал, имя, лор, glow, custom-model-data, скрытые флаги, доп. PDC).【F:src/main/resources/config.yml†L110-L139】【F:src/main/java/dev/byflow/customtntflow/model/RegionTNTType.java†L65-L105】
* Поведение взрыва описывается через миксины, наследование и дефолты (version 2): порядок применения — тип → `use` слева направо → `extends` (рекурсивно) → `defaults`; списки можно объединять с dedupe, карты с глубоким merge.【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L36-L167】【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L466-L520】
* Полный контроль логики: радиус/форма (сфера или куб), whitelist/blacklist, пропуск обсидиана, разрешение жидкостей, лимит блоков, drop-blacklist, `api-only` и др.【F:src/main/resources/config.yml†L19-L139】【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L358-L414】
* TNT автоматически заменяется сущностью при установке (если включён `ignite-when-placed`), а в чанках блоки ломаются пакетами по 3000 за тик без форс-лоуда.【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L52-L163】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L182-L227】
* Повреждение сущностей выполняется через «пустой» ванильный взрыв, поэтому работают щиты, броня и другие плагины, слушающие урон от TNT.【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L147-L179】
* Региональные проверки WorldGuard/ProtectionStones с режимами STRICT/LENIENT и кастомным сообщением запрета.【F:src/main/resources/config.yml†L7-L15】【F:src/main/java/dev/byflow/customtntflow/service/region/RegionIntegrationService.java†L25-L70】【F:src/main/java/dev/byflow/customtntflow/service/region/RegionCheckMode.java†L1-L15】
* Стабильные PDC-метки (`customtntflow:type/traits/uuid/owner/explode_id`) на предметах и сущностях + JSON-снимок параметров для внешних интеграций.【F:src/main/java/dev/byflow/customtntflow/util/PersistentDataKeys.java†L11-L48】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L136-L235】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L370-L389】
* Полноценный API: методы `RegionTNTAPI`, цепочка событий (Prime → PreAffect → Affect → RegionTNTDetonate → CustomTNTExplode) и mutable-обёртка поведения для runtime-патчей.【F:src/main/java/dev/byflow/customtntflow/api/RegionTNTAPI.java†L12-L85】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L60-L164】【F:src/main/java/dev/byflow/customtntflow/api/MutableBlockBehavior.java†L12-L193】
* Диагностика: режимы debug, команды `/tntflow list|info|reload|debug`, логирование собранных типов/миксинов, подсказки по ошибкам конфигурации.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L38-L215】【F:src/main/java/dev/byflow/customtntflow/model/DebugSettings.java†L6-L49】【F:src/main/resources/config.yml†L8-L15】

## Команды и права

Команда `/tntflow` (алиас `/regiontnt`) доступна операторам с правом `customtntflow.admin`.【F:src/main/resources/plugin.yml†L7-L16】

| Команда | Назначение |
|---------|------------|
| `/tntflow` | Краткая справка по подкомандам.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L38-L64】 |
| `/tntflow give <player> <type> [amount]` | Выдаёт игроку кастомный TNT, автоматически прописывая PDC и лор-плейсхолдеры.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L66-L101】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L136-L174】 |
| `/tntflow list` | Показывает все типы с указанием источника `extends` и подключённых миксинов.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L103-L126】 |
| `/tntflow info <type>` | Детализированная сводка по предмету, наследованию, миксинам и всем флагам поведения.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L128-L168】 |
| `/tntflow reload` | Перечитывает конфиг, выводит статистику по типам, миксинам, ошибкам/предупреждениям.【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L170-L191】 |
| `/tntflow debug [flag]` | Показывает состояние debug-флагов или переключает выбранный (`log-compiled-types`, `log-merge-sources`, `trace-explosion`).【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L194-L215】【F:src/main/java/dev/byflow/customtntflow/model/DebugFlag.java†L5-L31】 |

## Структура конфигурации

Главный файл `config.yml` состоит из нескольких блоков.【F:src/main/resources/config.yml†L1-L143】 

### `messages`
Сообщения игроку. Плейсхолдеры `{type}` и `{radius}` подставляются при установке TNT.【F:src/main/resources/config.yml†L1-L5】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L81-L88】

### `settings`
* `debug` — стартовые значения debug-флагов (см. выше).【F:src/main/resources/config.yml†L7-L12】
* `region-checks` — включает интеграции, режим `STRICT`/`LENIENT` и текст отказа, который отправляется игроку один раз за взрыв.【F:src/main/resources/config.yml†L12-L15】【F:src/main/java/dev/byflow/customtntflow/service/region/RegionIntegrationService.java†L25-L70】

### `version`
Отсутствует — используется legacy-парсер (`item/primed/behavior` без миксинов). При `version: 2` активируются `defaults`, `mixins`, `extends`, `use` и `merge` стратегии.【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L36-L116】

### `defaults`
Глобальные значения, которые применяются ко всем типам перед наследованием и миксинами (пример: включённый дроп, blacklist камня/руд).【F:src/main/resources/config.yml†L19-L62】

### `mixins`
Повторно используемые куски конфига. Миксины применяются в обратном порядке (последний в списке `use` выигрывает). Можно параметризовать миксин прямо в `use`, как в примере `radius15` с override на 18 блоков.【F:src/main/resources/config.yml†L64-L139】【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L152-L167】

### `types`
Определения TNT. Каждый тип может:
* Наследоваться через `extends`.
* Подключать миксины (`use: [a, b]` или `- { radius15: { behavior: { radius: 18 } } }`).
* Переопределять любые поля `item`, `primed`, `behavior`.

#### Раздел `item`
Материал, отображаемое имя, лор, glow, custom-model-data, unbreakable, скрытые флаги и произвольные `persistent-data` (ключи в формате `namespace:key`). Плейсхолдеры `{radius}`, `{drops}`, `{obsidian}`, `{water}` автоматически заменяются на актуальные значения типа.【F:src/main/resources/config.yml†L110-L139】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L136-L170】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L374-L385】

#### Раздел `primed`
Имя сущности, видимость, длительность фитиля (`fuse-ticks`), сила взрыва (`power`), флаги `incendiary`, `gravity`, `explode-in-water`, scoreboard-tags и дополнительный PDC для сущности.【F:src/main/resources/config.yml†L19-L71】【F:src/main/java/dev/byflow/customtntflow/model/RegionTNTType.java†L78-L85】

#### Раздел `behavior`
Ключевые параметры взрыва:
* `ignite-when-placed` — мгновенный поджог блока при установке.【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L60-L89】
* `radius` + `shape` (`SPHERE`/`CUBE`).【F:src/main/java/dev/byflow/customtntflow/service/explosion/GeometryStage.java†L10-L37】【F:src/main/java/dev/byflow/customtntflow/model/RegionTNTType.java†L87-L105】
* `break-blocks`, `drop-blocks`, `drop-blacklist` — управление разрушением и выпадением лута.【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L358-L379】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L157-L227】
* `whitelist-only`, `whitelist`, `blacklist`, `allow-obsidian`, `allow-crying-obsidian`, `allow-fluids` — фильтры этапа Filters.【F:src/main/java/dev/byflow/customtntflow/service/explosion/FilterStage.java†L11-L50】
* `max-blocks` — жёсткий лимит; если превышен, остаток обрезается, а в лог пишется предупреждение.【F:src/main/java/dev/byflow/customtntflow/service/explosion/FinalizeStage.java†L10-L18】
* `api-only` — плагин не ломает блоки, но события и PDC продолжают генерироваться (значение `break-blocks` игнорируется).【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L382-L413】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L147-L163】

### `merge`
* `lists: append-dedupe` — объединяет списки с устранением дублей (например, drop/blacklist).
* `maps: deep` — глубокий merge без потери вложенности (можно заменить на `replace`).【F:src/main/resources/config.yml†L141-L143】【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L466-L520】

## Примеры конфигов

В дефолтном `config.yml` показаны комбинации:
* `radius15_water_obsidian` — радиус 15 + работа под водой + ломание обсидиана.
* `obsidian_precision` — whitelist только обсидиан.
* `tire_hydro_black` — «чёрный» тип с дополнительным миксином воды.
* `tire_white_big` — расширение радиуса через параметризованный миксин.【F:src/main/resources/config.yml†L102-L139】

### Создание TNT без дропа и с собственным поведением

1. Добавьте миксин:
   ```yaml
   mixins:
     api_only_damage:
       behavior:
         break-blocks: false
         api-only: true
         drop-blocks: false
   ```
2. Подключите его к типу через `use`. Теперь TNT вызовет события, но не тронет блоки — можно обрабатывать урон приватных блоков в стороннем плагине.【F:src/main/java/dev/byflow/customtntflow/config/TypeConfigurationLoader.java†L152-L167】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L147-L163】

## Внутренний pipeline взрыва

Пайплайн состоит из четырёх стадий, которые выполняются в указанном порядке, если взрыв нужно просчитать (break-blocks, api-only или есть слушатели CustomTNTAffectEvent).【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L125-L133】【F:src/main/java/dev/byflow/customtntflow/service/explosion/ExplosionPipeline.java†L19-L38】

1. **Geometry** — собирает кандидатов по радиусу с учётом формы и лимита `max-blocks` (раннее завершение).【F:src/main/java/dev/byflow/customtntflow/service/explosion/GeometryStage.java†L10-L27】
2. **Filters** — выкидывает воздух, жидкости (если запрещены), блоки из blacklist и неразрешённый обсидиан/whitelist.【F:src/main/java/dev/byflow/customtntflow/service/explosion/FilterStage.java†L11-L50】
3. **Region checks** — сверяется с интеграциями (WorldGuard, ProtectionStones) и, при отказе, уведомляет игрока один раз за взрыв.【F:src/main/java/dev/byflow/customtntflow/service/explosion/RegionCheckStage.java†L16-L27】【F:src/main/java/dev/byflow/customtntflow/service/region/RegionIntegrationService.java†L25-L70】
4. **Finalize** — сортирует итоговый набор, обрезает по `max-blocks` и фиксирует результат для событий/логики разрушения.【F:src/main/java/dev/byflow/customtntflow/service/explosion/FinalizeStage.java†L10-L18】

После пайплайна плагин вызывает события, обновляет traits в PDC и, если `break-blocks=true`, асинхронно ломает блоки с уважением к уже загруженным чанкам и drop-черному списку.【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L132-L227】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L239-L244】

## Региональные интеграции

* Включаются через `settings.region-checks.enabled`.
* `mode: STRICT` — блоки исключаются, если хотя бы один плагин запрещает взрыв; `LENIENT` допускает владельца/доверенных (реализовано в хукках WorldGuard/ProtectionStones).
* `deny-message` отправляется один раз инициатору или владельцу TNT (если известен).【F:src/main/resources/config.yml†L7-L15】【F:src/main/java/dev/byflow/customtntflow/service/region/RegionIntegrationService.java†L25-L70】

## Persistent Data (PDC)

### ItemStack
* `customtntflow:type` — ID типа.
* `customtntflow:traits` — JSON с ключами `radius`, `shape`, `igniteWhenPlaced`, `breakBlocks`, `dropBlocks`, `dropBlacklist`, `allowFluids`, `allowObsidian`, `allowCryingObsidian`, `whitelistOnly`, `apiOnly`, `maxBlocks`, `whitelist`, `blacklist`.
* `customtntflow:uuid` — уникальный идентификатор предмета.
* Доп. ключи из `item.persistent-data` сохраняются как строковые значения (namespace:key).【F:src/main/java/dev/byflow/customtntflow/util/PersistentDataKeys.java†L11-L48】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L136-L170】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L370-L389】

### TNTPrimed
Все ключи предмета +
* `customtntflow:owner` — UUID игрока, поставившего TNT (если известен).
* `customtntflow:explode_id` — уникальный идентификатор конкретного взрыва.
* Доп. ключи из `primed.persistent-data` и scoreboard-tags.【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L211-L235】

Получить значения можно через Bukkit API либо методом `RegionTNTAPI.getEffectiveTraits(entity)` — вернёт Map из распарсенного traits JSON.【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L267-L281】【F:src/main/java/dev/byflow/customtntflow/api/RegionTNTAPI.java†L80-L85】

## API для интеграций

### RegionTNTAPI

```java
RegionTNTAPI api = RegionTNTAPI.get();
api.findType("tire_white");          // По ID из конфига
api.findType(itemStack);              // По предмету (PDC)
api.findType(entity);                 // По сущности TNTPrimed
api.isCustom(entity);                 // Проверка, что это кастомный TNT
api.getEffectiveTraits(entity);       // Снимок поведения текущего взрыва
```
【F:src/main/java/dev/byflow/customtntflow/api/RegionTNTAPI.java†L35-L85】

Для удобства можно использовать `EntityOrItem.of(...)` при работе с универсальными контейнерами.【F:src/main/java/dev/byflow/customtntflow/api/EntityOrItem.java†L8-L41】

### События (порядок вызова)

1. **CustomTNTPrimeEvent** — блок превращается в TNTPrimed; можно отменить или заменить предмет/игрока в своей логике.【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTPrimeEvent.java†L13-L67】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L60-L89】
2. **CustomTNTPreAffectEvent** — перед сбором блоков. Доступ к `MutableBlockBehavior`, можно поменять радиус, включить `apiOnly`, разрешить жидкости и т. д. Отмена прекращает взрыв полностью.【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTPreAffectEvent.java†L10-L58】【F:src/main/java/dev/byflow/customtntflow/api/MutableBlockBehavior.java†L12-L193】
3. **CustomTNTAffectEvent** — выдаёт mutable-набор блоков после пайплайна (Geometry+Filters+Region). Можно удалить/добавить блоки перед финализацией.【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTAffectEvent.java†L13-L54】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L125-L142】
4. **RegionTNTDetonateEvent** — совместимое событие для старых аддонов; даёт список блоков, можно отменить финальный этап или модифицировать список.【F:src/main/java/dev/byflow/customtntflow/api/event/RegionTNTDetonateEvent.java†L15-L68】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L147-L163】
5. **CustomTNTExplodeEvent** — финальный список блоков, UUID владельца и прямой доступ к PDC перед нанесением урона и разрушением. Используйте для логирования или реакции без изменения списка.【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTExplodeEvent.java†L15-L61】【F:src/main/java/dev/byflow/customtntflow/listener/RegionTNTListener.java†L147-L163】

### Пример интеграции: «урон по приватным блокам без разрушения»

```java
public class ProtectionDamageListener implements Listener {
    private static final String TYPE_ID = "tire_white"; // или любой ваш тип

    @EventHandler
    public void onPreAffect(CustomTNTPreAffectEvent event) {
        if (!event.getType().getId().equals(TYPE_ID)) {
            return;
        }
        var behavior = event.getBehavior();
        behavior.setBreakBlocks(false);  // блоки не ломаем
        behavior.setApiOnly(true);       // заставляем плагин не трогать мир
        behavior.setDropBlocks(false);   // на всякий случай отключаем дроп
    }

    @EventHandler
    public void onExplode(CustomTNTExplodeEvent event) {
        if (!event.getType().getId().equals(TYPE_ID)) {
            return;
        }
        for (Block block : event.getBlocks()) {
            // Здесь ваша логика «урона» по приватным структурам
            // Доступны PDC, owner UUID и traits для вычислений
        }
    }
}
```
`CustomTNTPreAffectEvent` гарантирует, что даже если в конфиге тип ломает блоки, вы можете выключить разрушение на лету. Затем `CustomTNTExplodeEvent` отдаёт финальный список блоков и PDC, так что внешний плагин может вручную обрабатывать защиту территорий, начислять «прочность» и т. д.【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTPreAffectEvent.java†L10-L58】【F:src/main/java/dev/byflow/customtntflow/api/MutableBlockBehavior.java†L12-L193】【F:src/main/java/dev/byflow/customtntflow/api/event/CustomTNTExplodeEvent.java†L15-L61】

## Диагностика и отладка

* Первичная загрузка/перезагрузка конфига записывает количество типов, миксинов, предупреждений и ошибок; все ошибки/варнинги выводятся в лог сразу.【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L68-L94】
* `settings.debug.*` управляет автологированием при старте, а команда `/tntflow debug` позволяет переключать флаги на лету. Включённые флаги `log-compiled-types` и `log-merge-sources` немедленно печатают снэпшоты по всем типам.【F:src/main/resources/config.yml†L7-L12】【F:src/main/java/dev/byflow/customtntflow/model/DebugSettings.java†L6-L49】【F:src/main/java/dev/byflow/customtntflow/service/RegionTNTRegistry.java†L300-L401】【F:src/main/java/dev/byflow/customtntflow/service/command/RegionTNTCommand.java†L194-L215】
* `trace-explosion` включает пометку каждой стадии пайплайна и итогового количества блоков (в консоль сервера).【F:src/main/java/dev/byflow/customtntflow/model/DebugSettings.java†L6-L49】【F:src/main/java/dev/byflow/customtntflow/service/explosion/ExplosionPipeline.java†L19-L38】

## Сборка из исходников

1. Установите Java 21 и Gradle 8+ (wrapper не включён в репозиторий).
2. Выполните `gradle build` или `gradle copyPlugin`.
3. Скопируйте JAR из `build/libs` либо `build/output` в папку `plugins/` вашего Paper-сервера.

При возникновении вопросов можно ориентироваться на Sample-конфиг и приведённые выше примеры — все ключевые элементы API и форматы данных задокументированы, поэтому дополнительное изучение исходников не требуется.
