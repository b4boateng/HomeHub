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

    public void send(String deviceId, Command command) {
        Devices device = registry.get(deviceId);

        if (!(device instanceof Controllable controllable) || !controllable.supports(command.action())) {
            throw new HomeHubException(device.getName() + " cannot " + command.action());
        }

        if (!device.isOnline() && command.action() != Action.TURN_ON && command.action() != Action.TURN_OFF) {
            throw new DeviceOfflineException(device.getName());
        }

        if (command.action() == Action.TURN_ON && !device.isOnline()) {
            powerPolicy.require(currentLoad(), device.getRatedWatts());
        }

        controllable.execute(command);
    }
}
