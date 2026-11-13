package com.homehub;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class DeviceRegistry {
   private final Map<String, Devices> devices = new LinkedHashMap<>();

   public void register(Devices device) {
    if (devices.putIfAbsent(device.getId(), device) != null) {
        throw new IllegalArgumentException("Duplicate device id: " + device.getId());
    }
   }

   public Devices get(String id){
        Devices dev = devices.get(id);
        if (dev == null) {
            throw new DeviceNotFoundException(id);
        }
        return dev;
   }


   public boolean contains(String id) {
        return devices.containsKey(id);
    }

    public Collection<Devices> all() {
        return Collections.unmodifiableCollection(devices.values());
    }
}
