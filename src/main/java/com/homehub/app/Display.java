package com.homehub.app;

import java.io.PrintStream;
import java.util.Map;

import com.homehub.automation.Rule;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;

// Only formats output; it never changes any state.
class Display {

    private final PrintStream out;

    Display(PrintStream out) {
        this.out = out;
    }

    void devices(DeviceRegistry registry) {
        // Column widths follow the longest value, so long names do not push the other columns out of line.
        int idWidth = registry.all().stream().mapToInt(d -> d.getId().length()).max().orElse(0);
        int nameWidth = registry.all().stream().mapToInt(d -> d.getName().length()).max().orElse(0);
        int roomWidth = registry.all().stream().mapToInt(d -> d.getRoom().length()).max().orElse(0);
        String row = "%-" + idWidth + "s  %-" + nameWidth + "s  %-" + roomWidth + "s  %6.0f W  %s%n";
        for (Devices d : registry.all()) {
            out.printf(row, d.getId(), d.getName(), d.getRoom(), d.getRatedWatts(), d.isOnline() ? "ON" : "OFF");
        }
    }

    // Shows the device as it is now, so the user can see what a command did.
    void state(Devices d) {
        out.println(d.getName() + ": " + (d.isOnline() ? "ON" : "OFF") + d.describeDetails());
    }

    // Says what triggers each rule and what it does, and flags rules whose device no longer exists.
    void rules(Iterable<Rule> rules, DeviceRegistry registry) {
        for (Rule r : rules) {
            String id = r.getTargetDeviceId();
            String target = registry.contains(id) ? id + " (" + registry.get(id).getName() + ")" : id + " (no such device)";
            out.println("priority " + r.getPriority() + ": when " + r.getCondition().describe()
                    + " -> " + r.getCommand().action() + " " + target);
        }
    }

    void energy(Map<String, Double> kwhByDevice) {
        out.println("=== energy used (kWh) ===");
        kwhByDevice.forEach((id, kwh) -> out.printf("%-4s %.4f%n", id, kwh));
    }
}
