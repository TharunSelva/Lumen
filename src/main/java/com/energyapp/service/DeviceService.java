package com.energyapp.service;

import com.energyapp.model.Device;
import com.energyapp.model.DeviceType;
import com.energyapp.model.SmartPlug;

import java.util.ArrayList;
import java.util.List;

public class DeviceService {

    private List<Device> allDevices = new ArrayList<>();

    public DeviceService() {
        // instantiate a few dummy plugs for testing
        SmartPlug plug1 = new SmartPlug("Living Room", DeviceType.SMART_PLUG);
        SmartPlug plug2 = new SmartPlug("Kitchen", DeviceType.SMART_PLUG);
        SmartPlug plug3 = new SmartPlug("Bedroom", DeviceType.SMART_PLUG);
        allDevices.add(plug1); allDevices.add(plug2); allDevices.add(plug3);
    }

    public List<Device> getAllDevices() {
        return this.allDevices;
    }

}
