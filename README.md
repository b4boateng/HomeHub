# HomeHub

A console smart-home simulator written in Java 17. Devices (bulbs, TVs, speakers, locks, motion, temperature and humidity sensors) are loaded from CSV files at startup. A rule engine reacts to events (motion, door unlocked, ...) by sending commands to those devices, and an energy accountant records how much power each device used while the events play out.

Individual final project for the Java OOP course (JUNIA M1).

## How to run

Requirements: JDK 17 or newer and Maven 3.8+.

```bash
git clone https://github.com/b4boateng/HomeHub.git
cd HomeHub
mvn clean test        # compile and run all unit tests
mvn exec:java         # start the application (run from the project root)
```

The app reads its data from `src/main/resources` by default, so start it from the project root. To use another folder that holds the four CSV files:

```bash
mvn exec:java -Dexec.args="path/to/data-folder"
```

If a file is missing or a row is invalid, the app prints `Could not start: <file> line <n>: <problem>` and exits instead of crashing.

## Main features

Menu shown after startup:

| Option | What it does |
|---|---|
| 1 | List devices with room, rated power and ON/OFF state |
| 2 | List rules in plain words, flagging rules whose device no longer exists |
| 3 | Send a command to a device (turn on/off, lock/unlock, brighten/dim, warmer/cooler, play/stop/pause/resume). Unlocking from the console asks for the lock's code |
| 4 | Trigger an event by hand and watch the rules react |
| 5 | Run the simulation: replay `events.csv` in time order, apply the rules, bill energy hour by hour |
| 6 / 7 | Add / remove a device; the change is saved to `devices.csv` as well as memory |
| 8 | Read sensors: every online sensor takes a new simulated reading, shows it, and publishes it as an event so matching rules can react |
| 0 | Quit |

Business rules enforced by the engine:

- **Rules fire by priority.** Every rule matching an event runs, lowest priority number first.
- **Time windows.** A rule can be limited to a time of day. Start is inclusive, end is exclusive, and a window such as `22:00` to `06:00` wraps past midnight.
- **Conflict detection.** Two rules on the same device with the same priority but different actions are rejected when the rule is added, because the engine could not choose between them.
- **Device guards.** A device can only receive actions it supports (a lock cannot dim). An offline device accepts only `TURN_ON` and `TURN_OFF`. Turning a device on is refused if it would push the home above the 3000 W limit.
- **Device state.** Each lock or unlock uses 1% of the lock's battery, and a lock with a flat battery refuses commands. Bulbs keep brightness and colour temperature, and remember their last command.
- **Energy accounting.** During a simulation each device is billed for the time it spent on, split per clock hour. The result is merged into `energy.csv`; replaying the same period replaces those hours instead of counting them twice.

## Data files

All four files live in `src/main/resources` and are read at startup (energy history is read when a simulation runs).

| File | Columns |
|---|---|
| `devices.csv` | `type,id,name,brand,model,room,watts,online` (type is BULB, TV, SPEAKER, LOCK, MOTION, TEMPERATURE or HUMIDITY) |
| `rules.csv` | `eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority` (window cells may be empty) |
| `events.csv` | `timestamp,type,source,value` |
| `energy.csv` | `hour,deviceId,kwh` (written by the simulation) |

Devices loaded from the file start with default internal state: bulbs at brightness 50, locks locked with code `0000` and a full battery, media players at volume 20. No device or rule is created in code. Editing a CSV and restarting changes the behaviour of the app.

## Project structure

```
src/main/java/com/homehub/
  app/         Application (wiring only), Console (menu only), Prompter (reads and validates typed answers), Display (output formatting)
  service/     SimulationRunner (replay events and save energy history), DeviceCatalog (add/remove devices in memory and file),
               SensorService (read sensors and publish their readings as events)
  devices/     Devices (abstract), SmartBulb, MediaPlayer, SmartLock, MotionSensor,
               TemperatureSensor, HumiditySensor, Controllable, EnergyReporting, Playable, Sensing,
               Command, Action, DeviceRegistry
  automation/  Event, EventType, EventBus, Condition, EventCondition, TimeWindow,
               Rule, RuleEngine, DeviceControl, Simulation
  energy/      PowerPolicy, PowerManager, EnergyAccountant, EnergyRecord
  loaders/     CsvLoader<T>, DevicesLoader, RulesLoader, EventsLoader, EnergyLoader
  exceptions/  HomeHubException, DataLoadException
src/main/resources/   devices.csv, rules.csv, events.csv, energy.csv
src/test/java/        JUnit 5 tests, one package per main package
```

## Main classes

- **`Devices`**: abstract base holding what every device shares (id, name, brand, model, room, rated watts, online state).
- **`DeviceRegistry`**: keeps devices by id, rejects duplicate ids, throws on unknown ids.
- **`Event` / `EventBus`**: an event is an immutable fact (type, time, source, value); the bus delivers it to subscribers without the publisher knowing who listens.
- **`Rule` / `EventCondition` / `TimeWindow`**: a rule pairs a condition with a target device and a command.
- **`RuleEngine`**: subscribes to the bus, finds matching rules, runs them in priority order and logs what happened.
- **`DeviceControl`**: the single gate every command passes through (supported action, online check, power limit).
- **`Simulation` / `EnergyAccountant`**: replay events in time order and bill energy between them.
- **`SimulationRunner`**: runs a whole simulation (reads saved history, replays, merges and saves `energy.csv`) so the console does not have to.
- **`SensorService`**: reads every online sensor once and publishes each reading as an event, so rules can react to it.
- **`DeviceCatalog`**: adds and removes devices so the registry and `devices.csv` always change together, undoing the in-memory change if the file write fails.
- **`CsvLoader<T>`** and its four subclasses: turn CSV rows into domain objects.
- **`Application`**: loads the files, builds the objects, hands over to `Console`. It holds no business logic.

## Technical decisions

1. **One abstract class plus small interfaces, instead of a deep hierarchy.** `Devices` holds the shared state, while capabilities are interfaces: `Controllable` (can receive commands), `EnergyReporting` (draws power), `Playable` (media), `Sensing` (sensors). The three sensors extend `Devices` but are not `Controllable`, so they can never be sent a command, and no subclass has to implement methods it cannot support. An abstract class fits the shared fields; interfaces fit capabilities that only some devices have.
2. **Rules are data, and the engine reacts to events through a bus.** `Condition` is an interface and `EventCondition` is built from a `rules.csv` row, so the engine knows nothing about specific devices or events. `RuleEngine` subscribes to `EventBus`, which means the console, the simulation and tests can all publish events the same way.
3. **All commands go through `DeviceControl`, and the power limit is injected as a `PowerPolicy`.** Manual commands from the menu and commands from rules share the same checks, so a rule cannot do something the user could not. The limit is an interface, so changing how it works (for example a time-of-day limit) does not touch `DeviceControl`.
4. **A generic `CsvLoader<T>` with unchecked, specific errors.** The base class does the reading, column count check and line numbering once; each subclass only implements `parseRow`. Any failure becomes a `DataLoadException` naming the file and line, which `Application` reports at startup. Exceptions extend `RuntimeException`, so intermediate layers are not forced to declare or catch them, and they are caught only where something useful can be done (the menu loop, the rule engine, `Application`).

## Where inheritance is used

- `Devices` is the parent of `SmartBulb`, `MediaPlayer` (used for both TVs and speakers, since they behave the same), `SmartLock`, `MotionSensor`, `TemperatureSensor` and `HumiditySensor`.
- `CsvLoader<T>` is the parent of `DevicesLoader`, `RulesLoader`, `EventsLoader` and `EnergyLoader`.
- `HomeHubException` is the parent of `DataLoadException`.

## Where polymorphism is used

- `DeviceRegistry` stores everything as `Devices`. `DeviceControl` calls `supports(action)` and `execute(command)` through `Controllable`, and each class answers differently: a bulb handles `BRIGHTEN` and `DIM`, a lock handles `LOCK` and `UNLOCK`, a media player handles `PLAY` and `PAUSE`.
- Menu option 8 loops over the registry and calls `readValue()` and `getReading()` through `Sensing`; a motion, temperature and humidity sensor each produce their reading in their own way.
- `DeviceControl.currentLoad()` and `EnergyAccountant` call `currentPowerWatts()` through `EnergyReporting` on whatever devices report power, without checking their type.
- `Display` prints every device through `describeDetails()`, which each subclass overrides, and `Console` reaches sensors through `asSensor()`, so neither checks a device's class.
- `CsvLoader.load()` calls the abstract `parseRow()`, and each loader builds a different object.
- `DevicesLoader` returns the right `Devices` subclass for each CSV row.

## Where the abstract class and interfaces are used

| Type | Kind | Purpose |
|---|---|---|
| `Devices` | abstract class | State and getters shared by all devices |
| `CsvLoader<T>` | abstract class | Shared file reading; subclasses define the row parsing |
| `Controllable` | interface | Devices that can receive a `Command` |
| `EnergyReporting` | interface | Devices that report current power draw |
| `Playable` | interface | Play/stop/pause/resume behaviour |
| `Sensing` | interface | Devices that take a reading (`readValue()`) and report it as a whole number (`getReading()`) |
| `Condition` | interface | Anything that can decide whether an event matches a rule |
| `PowerPolicy` | interface | The rule for allowing extra load |

## Custom exceptions

- `HomeHubException` (unchecked): general business errors such as an unknown device id, a device that is offline, an action a device does not support, a wrong lock code, a flat lock battery, a power limit that would be exceeded, or two conflicting rules.
- `DataLoadException` (extends `HomeHubException`): a bad or unreadable data file, carrying the file name and the line number.

Standard exceptions are used for invalid arguments (`IllegalArgumentException` for blank ids, negative watts, duplicate device ids, time going backwards) and for I/O (`IOException` is caught in the loaders and rethrown as a `DataLoadException`, or as a `HomeHubException` when saving `energy.csv`).

## How the app loads its data

At startup `Application` resolves the data folder (default `src/main/resources`) and calls `DevicesLoader` for `devices.csv`, `RulesLoader` for `rules.csv` and `EventsLoader` for `events.csv`. Each loader skips the header and blank lines, checks the column count, trims every cell and converts the row into a `Device`, `Rule` or `Event`. `energy.csv` is read when a simulation runs and rewritten afterwards. Adding or removing a device from the menu also edits `devices.csv`, so the file and memory stay in step.

## How SOLID is applied

- **S**: each class has one job. `Display` only formats, `Prompter` only reads and checks typed answers, `Console` only routes menu choices, `SimulationRunner`, `DeviceCatalog` and `SensorService` hold the work behind the menu, `RuleEngine` only matches and runs rules, `DeviceControl` only guards commands, `EnergyAccountant` only bills energy, loaders only read files.
- **O**: new conditions (`Condition`), power limits (`PowerPolicy`) and file formats (`CsvLoader`) are added as new classes without editing the engine. A new device type needs a new class and one line in `DevicesLoader` (plus a line in `Display` if it should show extra detail).
- **L**: any `Devices` subclass can be stored, listed and billed through the base type. Devices that cannot be commanded are left out of `Controllable` instead of throwing from an inherited method.
- **I**: four small capability interfaces rather than one large `Device` interface.
- **D**: `DeviceControl` depends on `PowerPolicy`, `Rule` depends on `Condition`, and `RuleEngine` takes a log `Consumer<String>`. `Application` is the only place that picks concrete classes.

## How to run the tests

```bash
mvn test                              # all tests
mvn -Dtest=RuleEngineTest test        # one test class
```

Tests are JUnit 5 and live in `src/test/java`, mirroring the main packages. They cover normal behaviour, edge cases (time windows across midnight, a limit hit exactly, empty input, billing across hour boundaries, sensor readings staying inside their range) and invalid input using `assertThrows` (unknown ids, unsupported actions, offline devices, power limit, conflicting rules, malformed CSV rows). `SimulationRunnerTest`, `DeviceCatalogTest` and `SensorServiceTest` cover the service layer, including failed saves. `IntegrationTest` starts the real `Application` on a copy of the data files and drives it through the menu.
