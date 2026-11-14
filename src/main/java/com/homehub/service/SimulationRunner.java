package com.homehub.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.homehub.automation.Event;
import com.homehub.automation.EventBus;
import com.homehub.automation.Simulation;
import com.homehub.devices.DeviceRegistry;
import com.homehub.energy.EnergyAccountant;
import com.homehub.energy.EnergyRecord;
import com.homehub.loaders.EnergyLoader;

// Replays a list of events through the rules, bills the energy used and saves the history to energy.csv.
public class SimulationRunner {

    // What a run produced: kWh per device for this run, and how many hourly records energy.csv now holds.
    public record Result(Map<String, Double> kwhByDevice, int savedRecords) {
    }

    private final DeviceRegistry registry;
    private final EventBus bus;
    private final Path energyFile;

    public SimulationRunner(DeviceRegistry registry, EventBus bus, Path energyFile) {
        this.registry = registry;
        this.bus = bus;
        this.energyFile = energyFile;
    }

    public Result run(List<Event> events, Consumer<String> log) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("There are no events to run");
        }
        // Read the saved history first: if energy.csv is unreadable we stop before the run changes any device.
        EnergyLoader energyLoader = new EnergyLoader();
        List<EnergyRecord> saved = Files.exists(energyFile) ? energyLoader.load(energyFile) : List.of();

        // Start billing at the first event, otherwise the idle time before it would count as usage.
        LocalDateTime first = events.stream().map(Event::timestamp).min(Comparator.naturalOrder()).get();
        LocalDateTime last = events.stream().map(Event::timestamp).max(Comparator.naturalOrder()).get();

        EnergyAccountant accountant = new EnergyAccountant(registry, first.truncatedTo(ChronoUnit.MINUTES));
        Simulation simulation = new Simulation(bus, accountant, log);
        events.forEach(simulation::addEvent);
        simulation.run(last.plusHours(1));

        List<EnergyRecord> history = accountant.mergedWith(saved);
        energyLoader.save(energyFile, history);
        return new Result(accountant.getKwhByDevice(), history.size());
    }
}
