package com.homehub.app;

import java.io.PrintStream;
import java.util.Map;

import com.homehub.automation.Rule;
import com.homehub.devices.DeviceRegistry;
import com.homehub.devices.Devices;
import com.homehub.devices.SmartBulb;
import com.homehub.devices.SmartLock;
import com.homehub.devices.SmartSpeaker;
import com.homehub.devices.SmartTv;

// Only formats output; it never changes any state.
class Display {

    private final PrintStream out;

    Display(PrintStream out) {
        this.out = out;
    }

    void devices(DeviceRegistry registry) {
        for (Devices d : registry.all()) {
            out.printf("%-4s %-18s %-12s %6.0f W  %s%n",
                    d.getId(), d.getName(), d.getRoom(), d.getRatedWatts(), d.isOnline() ? "ON" : "OFF");
        }
    }

    // Shows the device as it is now, so the user can see what a command did.
    void state(Devices d) {
        StringBuilder sb = new StringBuilder(d.getName()).append(": ").append(d.isOnline() ? "ON" : "OFF");
        if (d instanceof SmartBulb b) {
            sb.append(", brightness ").append(b.getBrightness());
        } else if (d instanceof SmartLock l) {
            sb.append(", ").append(l.getLockStatus() ? "locked" : "unlocked");
        } else if (d instanceof SmartTv t) {
            sb.append(", volume ").append(t.getVolume()).append(t.isMuted() ? " (muted)" : "")
                    .append(", channel ").append(t.getChannel()).append(", ").append(t.getPlaybackState());
        } else if (d instanceof SmartSpeaker s) {
            sb.append(", volume ").append(s.getVolume()).append(s.isMuted() ? " (muted)" : "")
                    .append(", ").append(s.getPlaybackState());
        }
        out.println(sb);
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
