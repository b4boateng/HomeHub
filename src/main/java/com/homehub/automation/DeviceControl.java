package com.homehub.automation;

import com.homehub.energy.*;
import com.homehub.devices.*;
import com.homehub.exceptions.*;

public class DeviceControl {

    private final DeviceRegistry registry;
    private final PowerPolicy powerPolicy;

    public DeviceControl(DeviceRegistry registry, PowerPolicy powerPolicy) {
        this.registry = registry;
        this.powerPolicy = powerPolicy;
    }

    public double currentLoad() {
        double total = 0;
        for (Devices d : registry.all()) {
            if (d instanceof EnergyReporting meter) {
                total += meter.currentPowerWatts();
            }
        }
        return total;
    }

    // Throws if this device cannot be given this action at all (a sensor, or a lock asked to dim).
    public void requireSupports(String deviceId, Action action) {
        Devices device = registry.get(deviceId);
        if (!(device instanceof Controllable controllable) || !controllable.supports(action)) {
            throw new HomeHubException(device.getName() + " cannot " + action);
        }
    }

    public void send(String deviceId, Command command) {
        requireSupports(deviceId, command.action());
        Devices device = registry.get(deviceId);
        Controllable controllable = (Controllable) device;

        // Offline devices can only be turned on or off.
        if (!device.isOnline() && command.action() != Action.TURN_ON && command.action() != Action.TURN_OFF) {
            throw new HomeHubException(device.getName() + " is offline");
        }

        // Only switching on adds load, so check the limit only then.
        if (command.action() == Action.TURN_ON && !device.isOnline()) {
            powerPolicy.require(currentLoad(), device.getRatedWatts());
        }

        controllable.execute(command);
    }
}
