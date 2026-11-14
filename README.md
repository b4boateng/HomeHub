# HomeHub

A console smart-home simulator. Devices (bulbs, TVs, speakers, locks, and motion, temperature and humidity sensors) are loaded from CSV files. Rules react to events (such as motion or a door unlocking) by sending commands to devices, and the app tracks how much energy each device uses.

Individual final project for the Java OOP course (JUNIA M1).

## How to run

You need JDK 17+ and Maven.

```bash
git clone https://github.com/b4boateng/HomeHub.git
cd HomeHub
mvn clean test     # run the tests
mvn exec:java      # start the app (run it from the project root)
```

To use data files from another folder: `mvn exec:java -Dexec.args="path/to/folder"`.

## Main features

The app shows a menu:

1. List devices
2. List rules
3. Send a command to a device (unlocking asks for the lock's code)
4. Trigger an event by hand
5. Run the simulation (replays `events.csv`, applies the rules, and records the energy used)
6. Add a device (also saved to `devices.csv`)
7. Remove a device (also removed from `devices.csv`)
8. Read the sensors (each reading can trigger rules)

The rules the app enforces:

- Rules run by priority (lowest number first). A rule can be limited to a time of day, including across midnight (for example 22:00 to 06:00).
- Two rules for the same device with the same priority but different actions are rejected as a conflict.
- A device only accepts actions it supports. An offline device only accepts turn on and turn off. Turning something on is refused if the home would go over 3000 W.
- Energy is counted per device per hour and saved to `energy.csv`. Replaying the same period does not count it twice.

## Data files (in `src/main/resources`)

| File | Columns |
|---|---|
| `devices.csv` | `type,id,name,brand,model,room,watts,online` |
| `rules.csv` | `eventType,source,minValue,windowStart,windowEnd,deviceId,action,priority` |
| `events.csv` | `timestamp,type,source,value` |
| `energy.csv` | `hour,deviceId,kwh` (written by the simulation) |

Device types are BULB, TV, SPEAKER, LOCK, MOTION, TEMPERATURE and HUMIDITY.

## How the data is loaded

When the app starts, `Application` reads `devices.csv`, `rules.csv` and `events.csv` and builds the devices, rules and events from them. Nothing is created directly in code, so editing a file changes how the app behaves. `energy.csv` is read and updated when a simulation runs. If a file is missing or a row is wrong, the app prints a message with the file name and line number and stops.

## Main classes

- `Devices`: abstract parent of all devices (id, name, room, power, online state).
- `SmartBulb`, `MediaPlayer` (TVs and speakers), `SmartLock`, `MotionSensor`, `TemperatureSensor`, `HumiditySensor`: the device types.
- `DeviceRegistry`: stores devices by id.
- `Event`, `EventBus`: an event is something that happened; the bus passes it to whoever is listening.
- `Rule`, `EventCondition`, `TimeWindow`: when a rule applies and what it does.
- `RuleEngine`: finds the rules that match an event and runs them.
- `DeviceControl`: every command goes through it (supported action, online, power limit).
- `Simulation`, `EnergyAccountant`: replay events in time order and count energy.
- `SimulationRunner`, `DeviceCatalog`, `SensorService`: the work behind menu options 5 to 8.
- `CsvLoader<T>` and its four loaders: read the CSV files.
- `Console`, `Prompter`, `Display`: the menu, reading answers, and printing.
- `Application`: loads the files and wires everything together. It has no business logic.

## Technical decisions

1. **One abstract class plus small interfaces.** `Devices` holds what every device shares. What a device can do is described by small interfaces (`Controllable`, `EnergyReporting`, `Playable`, `Sensing`). A sensor is not `Controllable`, so it can never be sent a command.
2. **Rules come from data and react to events.** A rule is built from a CSV row, and the engine listens on an `EventBus`. The menu, the simulation and the sensors can all trigger rules the same way.
3. **All commands go through `DeviceControl`.** Commands typed in the menu and commands from rules get the same checks. The power limit is a `PowerPolicy` interface, so it can change without touching `DeviceControl`.
4. **The menu has no logic.** `Console` only asks questions and prints. The real work is in small classes (`SimulationRunner`, `DeviceCatalog`, `SensorService`), which are easy to test on their own.

## Where inheritance is used

- `Devices` is the parent of `SmartBulb`, `MediaPlayer`, `SmartLock`, `MotionSensor`, `TemperatureSensor` and `HumiditySensor`.
- `CsvLoader<T>` is the parent of the four loaders.
- `HomeHubException` is the parent of `DataLoadException`.

## Where polymorphism is used

- The registry holds `Devices`, and `DeviceControl` calls `execute()` and `supports()` through `Controllable`. A bulb, a lock and a media player each react differently to the same call.
- `EnergyAccountant` calls `currentPowerWatts()` on every `EnergyReporting` device without checking its type.
- `Display` prints each device with `describeDetails()`, and each device type writes its own details.
- Sensors are read through `Sensing`, so a motion, temperature or humidity sensor each give their reading their own way.
- Each loader overrides `parseRow()` to build a different object.

## Where the abstract class and interfaces are used

- Abstract classes: `Devices` (shared device state) and `CsvLoader<T>` (shared file reading).
- Interfaces: `Controllable` (can receive commands), `EnergyReporting` (reports power), `Playable` (media), `Sensing` (takes readings), `Condition` (decides if an event matches a rule), `PowerPolicy` (decides if more power may be used).

I used an abstract class where classes share data and code, and interfaces where unrelated classes just need to share an ability.

## Custom exceptions

- `HomeHubException`: business errors, such as an unknown device, an offline device, an unsupported action, a wrong lock code, a power limit that would be exceeded, or conflicting rules.
- `DataLoadException`: a bad or unreadable data file, with the file name and line number.

Standard exceptions are also used: `IllegalArgumentException` for invalid values, and `IOException`, which the loaders catch and turn into one of the exceptions above.

## How SOLID is applied

- **S**: each class has one job (for example `Display` only prints, `Prompter` only reads answers, `RuleEngine` only runs rules).
- **O**: new conditions, power policies and file loaders are new classes, not edits to the engine. A new device type needs a new class and one line in `DevicesLoader`.
- **L**: every device can be used through `Devices`. Devices that cannot be commanded are left out of `Controllable` instead of throwing errors.
- **I**: several small interfaces instead of one big one.
- **D**: `DeviceControl` depends on `PowerPolicy`, and `Rule` depends on `Condition`. Only `Application` picks the concrete classes.

## How to run the tests

```bash
mvn test                          # all tests
mvn -Dtest=RuleEngineTest test    # one test class
```

The JUnit 5 tests are in `src/test/java`, in the same packages as the code they test. They cover normal cases, edge cases (time windows across midnight, exactly hitting the power limit, energy across hour boundaries) and errors using `assertThrows` (unknown devices, offline devices, conflicting rules, bad CSV rows). `IntegrationTest` starts the real app and uses the menu.
